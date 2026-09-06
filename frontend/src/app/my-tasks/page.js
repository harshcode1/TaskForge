'use client';

import { useState, useEffect } from 'react';
import ProtectedRoute from '@/components/auth/ProtectedRoute';
import Navbar from '@/components/layout/Navbar';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { tasksAPI } from '@/services/api';
import { priorityBadgeClass, priorityLabel, statusBadgeClass, statusLabel } from '@/lib/task-ui';
import { toast } from 'react-hot-toast';
import Link from 'next/link';
import {
  CheckCircle,
  Clock,
  AlertCircle,
  Loader2,
  ExternalLink,
  Calendar,
  Flag,
  ClipboardList,
} from 'lucide-react';

const STATUS_TABS = [
  { key: 'ALL', label: 'All' },
  { key: 'TODO', label: 'To Do' },
  { key: 'IN_PROGRESS', label: 'In Progress' },
  { key: 'PENDING', label: 'Pending' },
  { key: 'DONE', label: 'Done' },
];

export default function MyTasksPage() {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('ALL');

  useEffect(() => {
    fetchMyTasks();
  }, []);

  const fetchMyTasks = async () => {
    try {
      // GET /api/tasks — returns List<TaskDTO> assigned to the calling user
      const response = await tasksAPI.getMyTasks();
      setTasks(response.data);
    } catch (error) {
      console.error('Error fetching my tasks:', error);
      toast.error('Failed to load your tasks');
    } finally {
      setLoading(false);
    }
  };

  const filteredTasks = activeTab === 'ALL'
    ? tasks
    : tasks.filter(t => t.status === activeTab);

  const getStatusIcon = (status) => {
    switch (status) {
      case 'TODO': return <Clock className="h-3 w-3" />;
      case 'IN_PROGRESS': return <AlertCircle className="h-3 w-3" />;
      case 'PENDING': return <Loader2 className="h-3 w-3" />;
      case 'DONE': return <CheckCircle className="h-3 w-3" />;
      default: return <Clock className="h-3 w-3" />;
    }
  };

  const isOverdue = (dueDate, status) => {
    if (!dueDate || status === 'DONE') return false;
    return new Date(dueDate) < new Date();
  };

  const countByStatus = (status) => tasks.filter(t => t.status === status).length;

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

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-background">
        <Navbar />
        <div className="container mx-auto px-4 py-8">
          {/* Page Header */}
          <div className="mb-8">
            <h1 className="text-3xl font-bold">My Tasks</h1>
            <p className="text-muted-foreground mt-1">
              All tasks assigned to you across all projects
            </p>
          </div>

          {/* Summary Stats */}
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-8">
            <Card>
              <CardContent className="pt-4 text-center">
                <div className="text-2xl font-bold">{tasks.length}</div>
                <div className="text-sm text-muted-foreground">Total</div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-4 text-center">
                <div className="text-2xl font-bold text-muted-foreground">{countByStatus('TODO')}</div>
                <div className="text-sm text-muted-foreground">To Do</div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-4 text-center">
                <div className="text-2xl font-bold text-[#6ea8fe]">{countByStatus('IN_PROGRESS')}</div>
                <div className="text-sm text-muted-foreground">In Progress</div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-4 text-center">
                <div className="text-2xl font-bold text-primary">{countByStatus('PENDING')}</div>
                <div className="text-sm text-muted-foreground">Pending</div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-4 text-center">
                <div className="text-2xl font-bold text-[#5fd39a]">{countByStatus('DONE')}</div>
                <div className="text-sm text-muted-foreground">Done</div>
              </CardContent>
            </Card>
          </div>

          {/* Status Filter Tabs */}
          <div className="flex flex-wrap gap-2 mb-6">
            {STATUS_TABS.map((tab) => {
              const count = tab.key === 'ALL' ? tasks.length : countByStatus(tab.key);
              return (
                <Button
                  key={tab.key}
                  variant={activeTab === tab.key ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => setActiveTab(tab.key)}
                >
                  {tab.label}
                  <Badge
                    variant="secondary"
                    className="ml-2 h-5 min-w-5 text-xs"
                  >
                    {count}
                  </Badge>
                </Button>
              );
            })}
          </div>

          {/* Task List */}
          {filteredTasks.length === 0 ? (
            <div className="text-center py-16">
              <ClipboardList className="h-16 w-16 text-muted-foreground mx-auto mb-4" />
              <h3 className="text-xl font-semibold mb-2">
                {tasks.length === 0 ? 'No tasks assigned to you yet' : `No ${activeTab.replace('_', ' ').toLowerCase()} tasks`}
              </h3>
              <p className="text-muted-foreground mb-6">
                {tasks.length === 0
                  ? 'Ask a project owner to assign tasks to you, or create a task in a project you own.'
                  : 'Try a different filter to see your tasks.'}
              </p>
              <Link href="/projects">
                <Button>Go to Projects</Button>
              </Link>
            </div>
          ) : (
            <div className="space-y-3">
              {filteredTasks.map((task) => {
                const overdue = isOverdue(task.dueDate, task.status);
                return (
                  <Card key={task.id} className={`transition-colors hover:border-primary/40 ${overdue ? 'border-destructive/40' : ''}`}>
                    <CardContent className="py-4">
                      <div className="flex items-start justify-between gap-4">
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 flex-wrap mb-1">
                            <h3 className={`font-medium ${overdue ? 'text-destructive' : ''}`}>
                              {task.title}
                            </h3>
                            {overdue && (
                              <span className="priority-badge-high">
                                Overdue
                              </span>
                            )}
                          </div>
                          {task.description && (
                            <p className="text-sm text-muted-foreground line-clamp-2 mb-2">
                              {task.description}
                            </p>
                          )}
                          <div className="flex items-center gap-3 flex-wrap text-xs text-muted-foreground">
                            {task.dueDate && (
                              <span className={`flex items-center gap-1 ${overdue ? 'text-destructive font-medium' : ''}`}>
                                <Calendar className="h-3 w-3" />
                                Due {new Date(task.dueDate).toLocaleDateString()}
                              </span>
                            )}
                          </div>
                        </div>

                        <div className="flex items-center gap-2 flex-shrink-0 flex-wrap justify-end">
                          {/* Priority Badge */}
                          {task.priority && (
                            <span className={priorityBadgeClass(task.priority)}>
                              <Flag className="h-3 w-3" />
                              {priorityLabel(task.priority)}
                            </span>
                          )}

                          {/* Status Badge */}
                          <span className={statusBadgeClass(task.status)}>
                            {getStatusIcon(task.status)}
                            {statusLabel(task.status)}
                          </span>

                          {/* View in Project */}
                          {task.projectId && (
                            <Link href={`/projects/${task.projectId}`}>
                              <Button variant="outline" size="sm" className="flex items-center gap-1">
                                <ExternalLink className="h-3 w-3" />
                                View Project
                              </Button>
                            </Link>
                          )}
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </ProtectedRoute>
  );
}
