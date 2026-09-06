'use client';

import { useState, useEffect } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Sparkles, Loader2 } from 'lucide-react';
import { useAIStatus } from '@/hooks/use-ai-status';
import { aiAPI } from '@/services/api';
import { toast } from 'react-hot-toast';

export default function TaskModal({
  isOpen,
  onClose,
  onSave,
  task = null,
  projectMembers = [],
  projectName = '',
  loading = false
}) {
  const aiEnabled = useAIStatus();
  const [generating, setGenerating] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    status: 'TODO',
    priority: 'MEDIUM',
    assigneeEmail: '',
    dueDate: '',
  });

  useEffect(() => {
    if (task) {
      setFormData({
        title: task.title || '',
        description: task.description || '',
        status: task.status || 'TODO',
        priority: task.priority || 'MEDIUM',
        assigneeEmail: task.assigneeEmail || '',
        dueDate: task.dueDate ? new Date(task.dueDate).toISOString().split('T')[0] : '',
      });
    } else {
      setFormData({
        title: '',
        description: '',
        status: 'TODO',
        priority: 'MEDIUM',
        assigneeEmail: '',
        dueDate: '',
      });
    }
  }, [task, isOpen]);

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!formData.title.trim()) {
      alert('Title is required');
      return;
    }

    if (!formData.priority) {
      alert('Please select a priority');
      return;
    }

    const taskData = {
      ...formData,
      assigneeEmail: (!formData.assigneeEmail || formData.assigneeEmail === 'UNASSIGNED')
        ? null
        : formData.assigneeEmail,
      dueDate: formData.dueDate || null,
    };

    onSave(taskData);
  };

  const handleChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleGenerateDescription = async () => {
    if (!formData.title.trim()) {
      toast.error('Enter a title first — the description is generated from it');
      return;
    }
    setGenerating(true);
    try {
      const res = await aiAPI.generateTaskDescription(formData.title, projectName);
      handleChange('description', res.data.text);
    } catch (error) {
      toast.error(error.response?.data?.message || 'AI generation failed');
    } finally {
      setGenerating(false);
    }
  };

  const validProjectMembers = projectMembers.filter(
    (member) => member.user?.email
  );

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>
            {task ? 'Edit Task' : 'Create New Task'}
          </DialogTitle>
          <DialogDescription>
            {task
              ? 'Update the task details below.'
              : 'Fill in the details to create a new task.'
            }
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit}>
          <div className="space-y-4 py-4">
            {/* Title */}
            <div className="space-y-2">
              <Label htmlFor="title">Title *</Label>
              <Input
                id="title"
                placeholder="Enter task title"
                value={formData.title}
                onChange={(e) => handleChange('title', e.target.value)}
                required
              />
            </div>

            {/* Description */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="description">Description</Label>
                {aiEnabled && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-6 gap-1 px-2 text-xs text-primary hover:text-primary"
                    onClick={handleGenerateDescription}
                    disabled={generating}
                  >
                    {generating ? <Loader2 className="h-3 w-3 animate-spin" /> : <Sparkles className="h-3 w-3" />}
                    Generate with AI
                  </Button>
                )}
              </div>
              <Textarea
                id="description"
                placeholder="Enter task description"
                value={formData.description}
                onChange={(e) => handleChange('description', e.target.value)}
                rows={3}
              />
            </div>

            {/* Status and Priority */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="status">Status</Label>
                <Select value={formData.status} onValueChange={(value) => handleChange('status', value)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select status" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="TODO">To Do</SelectItem>
                    <SelectItem value="IN_PROGRESS">In Progress</SelectItem>
                    <SelectItem value="PENDING">Pending</SelectItem>
                    <SelectItem value="DONE">Done</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="priority">Priority</Label>
                <Select value={formData.priority} onValueChange={(value) => handleChange('priority', value)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select priority" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="LOW">Low</SelectItem>
                    <SelectItem value="MEDIUM">Medium</SelectItem>
                    <SelectItem value="HIGH">High</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Assignee */}
            <div className="space-y-2">
              <Label htmlFor="assignee">Assignee</Label>
              <Select onValueChange={(value) => handleChange('assigneeEmail', value)} value={formData.assigneeEmail}>
                <SelectTrigger>
                  <SelectValue placeholder="Select assignee" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="UNASSIGNED">Unassigned</SelectItem>
                  {projectMembers
                    .filter(member => member.user?.email)
                    .map((member) => (
                      <SelectItem key={member.user.email} value={member.user.email}>
                        {member.user.name} ({member.role})
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
            </div>

            {/* Due Date */}
            <div className="space-y-2">
              <Label htmlFor="dueDate">Due Date</Label>
              <Input
                id="dueDate"
                type="date"
                value={formData.dueDate}
                min={new Date().toISOString().split('T')[0]}
                onChange={(e) => handleChange('dueDate', e.target.value)}
              />
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? 'Saving...' : (task ? 'Update Task' : 'Create Task')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

