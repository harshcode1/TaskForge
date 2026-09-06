'use client';

import { useState, useEffect, useCallback } from 'react';
import dynamic from 'next/dynamic';
import { useParams, useRouter } from 'next/navigation';
import ProtectedRoute from '@/components/auth/ProtectedRoute';
import Navbar from '@/components/layout/Navbar';
import TaskModal from '@/components/tasks/TaskModal';

// @dnd-kit and recharts are only needed once a user is actually looking at this
// project's Board/Analytics tab — code-split them out of the initial bundle
// instead of shipping both on every page load (was ~180kB of unused JS on
// first paint; the tab content isn't needed until the user picks a tab).
const KanbanBoard = dynamic(() => import('@/components/tasks/KanbanBoard'), {
  ssr: false,
  loading: () => <div className="py-12 text-center text-muted-foreground">Loading board…</div>,
});
const ProjectAnalytics = dynamic(() => import('@/components/dashboard/ProjectAnalytics'), {
  ssr: false,
  loading: () => <div className="py-12 text-center text-muted-foreground">Loading analytics…</div>,
});
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { projectsAPI, tasksAPI, projectMembersAPI } from '@/services/api';
import { toast } from 'react-hot-toast';
import { Plus, Users, UserPlus, ArrowLeft, Pencil } from 'lucide-react';
import Link from 'next/link';

export default function ProjectDetailPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = params.id;

  const [project, setProject] = useState(null);
  const [tasks, setTasks] = useState([]);
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [taskModalOpen, setTaskModalOpen] = useState(false);
  const [editingTask, setEditingTask] = useState(null);
  const [inviteModalOpen, setInviteModalOpen] = useState(false);
  const [inviteData, setInviteData] = useState({ email: '', role: 'MEMBER' });
  const [inviting, setInviting] = useState(false);
  const [taskLoading, setTaskLoading] = useState(false);

  // Edit project state
  const [editProjectOpen, setEditProjectOpen] = useState(false);
  const [editProjectData, setEditProjectData] = useState({ name: '', description: '' });
  const [updatingProject, setUpdatingProject] = useState(false);

  const fetchProjectData = useCallback(async () => {
    try {
      const projectResponse = await projectsAPI.getById(projectId);
      setProject(projectResponse.data);

      const tasksResponse = await tasksAPI.getByProject(projectId);
      setTasks(tasksResponse.data);

      try {
        const membersResponse = await projectMembersAPI.getMembers(projectId);
        setMembers(membersResponse.data);
      } catch (error) {
        console.error('Error fetching members:', error);
        setMembers([]);
      }
    } catch (error) {
      console.error('Error fetching project data:', error);
      toast.error('Failed to load project data');
      router.push('/projects');
    } finally {
      setLoading(false);
    }
  }, [projectId, router]);

  useEffect(() => {
    if (!projectId) {
      toast.error('Invalid project URL');
      router.push('/projects');
      return;
    }
    fetchProjectData();
  }, [projectId, router, fetchProjectData]);

  const handleCreateTask = async (taskData) => {
    setTaskLoading(true);
    try {
      const response = await tasksAPI.create(
        projectId,
        taskData.title,
        taskData.description,
        taskData.dueDate,
        taskData.status,
        taskData.priority,
        taskData.assigneeEmail
      );
      setTasks([...tasks, response.data]);
      setTaskModalOpen(false);
      toast.success('Task created successfully!');
    } catch (error) {
      console.error('Error creating task:', error);
      toast.error('Failed to create task');
    } finally {
      setTaskLoading(false);
    }
  };

  const handleUpdateTask = async (taskId, taskData) => {
    setTaskLoading(true);
    try {
      const response = await tasksAPI.update(taskId, taskData);
      setTasks(tasks.map(t => t.id === taskId ? response.data : t));
      setTaskModalOpen(false);
      setEditingTask(null);
      toast.success('Task updated successfully!');
    } catch (error) {
      console.error('Error updating task:', error);
      toast.error('Failed to update task');
    } finally {
      setTaskLoading(false);
    }
  };

  const handleDeleteTask = async (taskId) => {
    if (!confirm('Are you sure you want to delete this task?')) return;
    try {
      await tasksAPI.delete(taskId);
      setTasks(tasks.filter(t => t.id !== taskId));
      toast.success('Task deleted successfully');
    } catch (error) {
      console.error('Error deleting task:', error);
      toast.error('Failed to delete task');
    }
  };

  const handleTaskEdit = (task) => {
    setEditingTask(task);
    setTaskModalOpen(true);
  };

  const handleTaskSave = (taskData) => {
    if (editingTask) {
      handleUpdateTask(editingTask.id, taskData);
    } else {
      handleCreateTask(taskData);
    }
  };

  const handleOpenEditProject = () => {
    setEditProjectData({ name: project.name, description: project.description || '' });
    setEditProjectOpen(true);
  };

  const handleUpdateProject = async (e) => {
    e.preventDefault();
    const name = editProjectData.name.trim();
    if (!name) {
      toast.error('Project name is required');
      return;
    }
    if (name.length < 3 || name.length > 100) {
      toast.error('Project name must be between 3 and 100 characters');
      return;
    }

    setUpdatingProject(true);
    try {
      const response = await projectsAPI.update(projectId, name, editProjectData.description);
      setProject(response.data);
      setEditProjectOpen(false);
      toast.success('Project updated successfully!');
    } catch (error) {
      console.error('Error updating project:', error);
      const status = error.response?.status;
      if (status === 403) {
        toast.error('Only the project owner can edit this project');
      } else {
        toast.error('Failed to update project');
      }
    } finally {
      setUpdatingProject(false);
    }
  };

  const handleInviteMember = async (e) => {
    e.preventDefault();
    const email = inviteData.email.trim();
    if (!email) {
      toast.error('Email is required');
      return;
    }

    setInviting(true);
    try {
      const response = await projectMembersAPI.invite(projectId, email, inviteData.role);
      setMembers((prev) => [...prev, response.data]);
      setInviteData({ email: '', role: 'MEMBER' });
      setInviteModalOpen(false);
      toast.success('Member invited successfully!');
    } catch (error) {
      console.error('Error inviting member:', error);
      const status = error.response?.status;
      if (status === 409) {
        toast.error('This user is already a member of this project');
      } else if (status === 403) {
        toast.error('Only the project owner can invite members');
      } else if (status === 404) {
        toast.error('No user found with that email address');
      } else {
        toast.error('Failed to invite member');
      }
    } finally {
      setInviting(false);
    }
  };

  const getTaskStats = () => {
    const total = tasks.length;
    const completed = tasks.filter(t => t.status === 'DONE').length;
    const inProgress = tasks.filter(t => t.status === 'IN_PROGRESS').length;
    const todo = tasks.filter(t => t.status === 'TODO').length;
    const pending = tasks.filter(t => t.status === 'PENDING').length;
    return { total, completed, inProgress, todo, pending };
  };

  const getRoleColor = (role) => {
    switch (role) {
      case 'ADMIN':
        return 'border-primary/30 bg-primary/10 text-primary';
      case 'MANAGER':
        return 'border-[#6ea8fe]/30 bg-[#6ea8fe]/10 text-[#6ea8fe]';
      case 'MEMBER':
        return 'border-border text-muted-foreground';
      default:
        return 'border-border text-muted-foreground';
    }
  };

  if (loading) {
    return (
      <ProtectedRoute>
        <div className="min-h-screen bg-background">
          <Navbar />
          <div className="container mx-auto px-4 py-8">
            <div className="flex items-center justify-center h-64">
              <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-primary"></div>
            </div>
          </div>
        </div>
      </ProtectedRoute>
    );
  }

  if (!project) {
    return (
      <ProtectedRoute>
        <div className="min-h-screen bg-background">
          <Navbar />
          <div className="container mx-auto px-4 py-8">
            <div className="text-center">
              <h1 className="text-2xl font-bold">Project not found</h1>
              <Link href="/projects">
                <Button className="mt-4">Back to Projects</Button>
              </Link>
            </div>
          </div>
        </div>
      </ProtectedRoute>
    );
  }

  const stats = getTaskStats();

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-background">
        <Navbar />
        <div className="container mx-auto px-4 py-8">
          {/* Header */}
          <div className="flex items-center mb-6">
            <Link href="/projects">
              <Button variant="ghost" size="icon" className="mr-4">
                <ArrowLeft className="h-4 w-4" />
              </Button>
            </Link>
            <div className="flex-1">
              <h1 className="text-3xl font-bold">{project.name}</h1>
              <p className="text-muted-foreground mt-1">
                {project.description || 'No description provided'}
              </p>
            </div>
            <div className="flex items-center space-x-2">
              {/* Edit Project */}
              <Button variant="outline" onClick={handleOpenEditProject}>
                <Pencil className="h-4 w-4 mr-2" />
                Edit Project
              </Button>

              <Button onClick={() => setTaskModalOpen(true)}>
                <Plus className="h-4 w-4 mr-2" />
                New Task
              </Button>

              {/* Invite Member Dialog */}
              <Dialog open={inviteModalOpen} onOpenChange={setInviteModalOpen}>
                <DialogTrigger asChild>
                  <Button variant="outline">
                    <UserPlus className="h-4 w-4 mr-2" />
                    Invite Member
                  </Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>Invite Team Member</DialogTitle>
                    <DialogDescription>
                      Invite a new member to collaborate on this project.
                    </DialogDescription>
                  </DialogHeader>
                  <form onSubmit={handleInviteMember}>
                    <div className="space-y-4 py-4">
                      <div className="space-y-2">
                        <Label htmlFor="email">Email Address</Label>
                        <Input
                          id="email"
                          type="email"
                          placeholder="Enter email address"
                          value={inviteData.email}
                          onChange={(e) => setInviteData({ ...inviteData, email: e.target.value })}
                          required
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="role">Role</Label>
                        <Select value={inviteData.role} onValueChange={(value) => setInviteData({ ...inviteData, role: value })}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select role" />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="MEMBER">Member</SelectItem>
                            <SelectItem value="MANAGER">Manager</SelectItem>
                            <SelectItem value="ADMIN">Admin</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                    </div>
                    <DialogFooter>
                      <Button type="button" variant="outline" onClick={() => setInviteModalOpen(false)}>
                        Cancel
                      </Button>
                      <Button type="submit" disabled={inviting}>
                        {inviting ? 'Inviting...' : 'Send Invite'}
                      </Button>
                    </DialogFooter>
                  </form>
                </DialogContent>
              </Dialog>
            </div>
          </div>

          {/* Stats Cards — 5 columns for all 4 statuses + total */}
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4 mb-8">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Total Tasks</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats.total}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">To Do</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-gray-600">{stats.todo}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">In Progress</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-[#6ea8fe]">{stats.inProgress}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Pending</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-primary">{stats.pending}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Completed</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-[#5fd39a]">{stats.completed}</div>
              </CardContent>
            </Card>
          </div>

          {/* Tabs */}
          <Tabs defaultValue="board" className="space-y-4">
            <TabsList>
              <TabsTrigger value="board">Board</TabsTrigger>
              <TabsTrigger value="members">Members ({members.length})</TabsTrigger>
              <TabsTrigger value="analytics">Analytics</TabsTrigger>
            </TabsList>

            <TabsContent value="board" className="space-y-4">
              <KanbanBoard
                tasks={tasks}
                onTaskUpdate={handleUpdateTask}
                onTaskEdit={handleTaskEdit}
                onTaskDelete={handleDeleteTask}
              />
            </TabsContent>

            <TabsContent value="members" className="space-y-4">
              <Card>
                <CardHeader>
                  <CardTitle>Project Members</CardTitle>
                  <CardDescription>
                    Manage team members and their roles in this project
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  {members.length === 0 ? (
                    <div className="text-center py-8">
                      <Users className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                      <p className="text-muted-foreground mb-4">No members yet</p>
                      <Dialog open={inviteModalOpen} onOpenChange={setInviteModalOpen}>
                        <DialogTrigger asChild>
                          <Button>
                            <UserPlus className="h-4 w-4 mr-2" />
                            Invite First Member
                          </Button>
                        </DialogTrigger>
                      </Dialog>
                    </div>
                  ) : (
                    <div className="space-y-4">
                      {members.map((member) => (
                        <div key={member.id} className="flex items-center justify-between p-3 border rounded-lg">
                          <div>
                            <p className="font-medium">{member.user?.name || member.user?.email}</p>
                            <p className="text-sm text-muted-foreground">
                              {member.user?.email}
                            </p>
                            <p className="text-xs text-muted-foreground">
                              Joined {new Date(member.joinedAt || Date.now()).toLocaleDateString()}
                            </p>
                          </div>
                          <Badge className={getRoleColor(member.role)}>
                            {member.role}
                          </Badge>
                        </div>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="analytics" className="space-y-4">
              <ProjectAnalytics
                projectId={projectId}
                tasks={tasks}
                members={members}
              />
            </TabsContent>
          </Tabs>

          {/* Task Modal */}
          <TaskModal
            isOpen={taskModalOpen}
            onClose={() => {
              setTaskModalOpen(false);
              setEditingTask(null);
            }}
            onSave={handleTaskSave}
            task={editingTask}
            projectMembers={members.filter(member => member.role)}
            loading={taskLoading}
          />

          {/* Edit Project Dialog */}
          <Dialog open={editProjectOpen} onOpenChange={setEditProjectOpen}>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Edit Project</DialogTitle>
                <DialogDescription>
                  Update the project name and description.
                </DialogDescription>
              </DialogHeader>
              <form onSubmit={handleUpdateProject}>
                <div className="space-y-4 py-4">
                  <div className="space-y-2">
                    <Label htmlFor="edit-project-name">Project Name *</Label>
                    <Input
                      id="edit-project-name"
                      placeholder="Enter project name"
                      value={editProjectData.name}
                      onChange={(e) => setEditProjectData({ ...editProjectData, name: e.target.value })}
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-project-desc">Description</Label>
                    <Textarea
                      id="edit-project-desc"
                      placeholder="Enter project description"
                      value={editProjectData.description}
                      onChange={(e) => setEditProjectData({ ...editProjectData, description: e.target.value })}
                      rows={3}
                    />
                  </div>
                </div>
                <DialogFooter>
                  <Button type="button" variant="outline" onClick={() => setEditProjectOpen(false)}>
                    Cancel
                  </Button>
                  <Button type="submit" disabled={updatingProject}>
                    {updatingProject ? 'Saving...' : 'Save Changes'}
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
        </div>
      </div>
    </ProtectedRoute>
  );
}
