'use client';

import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Separator } from '@/components/ui/separator';
import CommentSection from './CommentSection';
import { Calendar, User, Flag, Clock, MessageCircle, Edit, Trash2 } from 'lucide-react';
import { priorityBadgeClass, priorityLabel, statusBadgeClass, statusLabel } from '@/lib/task-ui';

export default function TaskDetailModal({ 
  task, 
  isOpen, 
  onClose, 
  onEdit, 
  onDelete,
  projectMembers = [] 
}) {
  const [showComments, setShowComments] = useState(false);

  if (!task) return null;

  const getInitials = (name) => {
    return name
      ?.split(' ')
      .map(word => word[0])
      .join('')
      .toUpperCase() || 'U';
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'No due date';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  const isOverdue = (dueDate) => {
    if (!dueDate) return false;
    return new Date(dueDate) < new Date() && task.status !== 'DONE';
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[700px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <DialogTitle className="text-xl font-semibold pr-8">
                {task.title}
              </DialogTitle>
              <div className="flex items-center space-x-2 mt-2">
                <span className={statusBadgeClass(task.status)}>
                  {statusLabel(task.status)}
                </span>
                {task.priority && (
                  <span className={priorityBadgeClass(task.priority)}>
                    <Flag className="h-3 w-3" />
                    {priorityLabel(task.priority)}
                  </span>
                )}
              </div>
            </div>
            <div className="flex items-center space-x-2">
              <Button variant="outline" size="sm" onClick={() => onEdit(task)}>
                <Edit className="h-4 w-4 mr-1" />
                Edit
              </Button>
              <Button variant="outline" size="sm" onClick={() => onDelete(task.id)}>
                <Trash2 className="h-4 w-4 mr-1" />
                Delete
              </Button>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-6">
          {/* Description */}
          {task.description && (
            <div>
              <h3 className="font-medium mb-2">Description</h3>
              <p className="text-muted-foreground whitespace-pre-wrap">
                {task.description}
              </p>
            </div>
          )}

          <Separator />

          {/* Task Details */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Assignee */}
            <div className="flex items-center space-x-3">
              <User className="h-4 w-4 text-muted-foreground" />
              <div>
                <p className="text-sm font-medium">Assignee</p>
                {task.assigneeEmail ? (
                  <div className="flex items-center space-x-2 mt-1">
                    <Avatar className="h-6 w-6">
                      <AvatarFallback className="text-xs">
                        {getInitials(task.assigneeEmail)}
                      </AvatarFallback>
                    </Avatar>
                    <span className="text-sm text-muted-foreground">
                      {task.assigneeEmail}
                    </span>
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground mt-1">Unassigned</p>
                )}
              </div>
            </div>

            {/* Due Date */}
            <div className="flex items-center space-x-3">
              <Calendar className="h-4 w-4 text-muted-foreground" />
              <div>
                <p className="text-sm font-medium">Due Date</p>
                <p className={`text-sm mt-1 ${
                  isOverdue(task.dueDate) 
                    ? 'text-destructive font-medium'
                    : 'text-muted-foreground'
                }`}>
                  {formatDate(task.dueDate)}
                  {isOverdue(task.dueDate) && ' (Overdue)'}
                </p>
              </div>
            </div>

            {/* Created Date */}
            <div className="flex items-center space-x-3">
              <Clock className="h-4 w-4 text-muted-foreground" />
              <div>
                <p className="text-sm font-medium">Created</p>
                <p className="text-sm text-muted-foreground mt-1">
                  {formatDate(task.createdAt)}
                </p>
              </div>
            </div>

            {/* Updated Date */}
            {task.updatedAt && task.updatedAt !== task.createdAt && (
              <div className="flex items-center space-x-3">
                <Clock className="h-4 w-4 text-muted-foreground" />
                <div>
                  <p className="text-sm font-medium">Last Updated</p>
                  <p className="text-sm text-muted-foreground mt-1">
                    {formatDate(task.updatedAt)}
                  </p>
                </div>
              </div>
            )}
          </div>

          <Separator />

          {/* Comments Section Toggle */}
          <div className="flex items-center justify-between">
            <Button
              variant="outline"
              onClick={() => setShowComments(!showComments)}
              className="flex items-center space-x-2"
            >
              <MessageCircle className="h-4 w-4" />
              <span>{showComments ? 'Hide Comments' : 'Show Comments'}</span>
            </Button>
          </div>

          {/* Comments */}
          <CommentSection
            taskId={task.id}
            isOpen={showComments}
            onClose={() => setShowComments(false)}
          />
        </div>
      </DialogContent>
    </Dialog>
  );
}

