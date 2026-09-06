'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useAuth } from '@/contexts/AuthContext';
import ProtectedRoute from '@/components/auth/ProtectedRoute';
import Navbar from '@/components/layout/Navbar';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import AnimatedNumber from '@/components/ui/animated-number';
import { projectsAPI, tasksAPI, dashboardAPI } from '@/services/api';
import { toast } from 'react-hot-toast';
import Link from 'next/link';
import { Plus, FolderOpen, CheckCircle, Clock, AlertCircle, Loader2 } from 'lucide-react';
import { statusBadgeClass, statusLabel } from '@/lib/task-ui';

const cardVariants = {
  hidden: { opacity: 0, y: 14 },
  visible: (i = 0) => ({
    opacity: 1,
    y: 0,
    transition: { duration: 0.4, delay: i * 0.06, ease: [0.25, 0.46, 0.45, 0.94] },
  }),
};

export default function DashboardPage() {
  const { user } = useAuth();
  const [projects, setProjects] = useState([]);
  const [recentTasks, setRecentTasks] = useState([]);
  const [stats, setStats] = useState({
    totalProjects: 0,
    totalTasks: 0,
    completedTasks: 0,
    inProgressTasks: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      // Run all 3 calls in parallel — was N+1, now always exactly 3
      const [projectsResponse, dashboardResponse, myTasksResponse] = await Promise.all([
        projectsAPI.getAll(),
        dashboardAPI.getUserDashboard().catch(() => null), // graceful fallback
        tasksAPI.getMyTasks(),
      ]);

      const projectsData = projectsResponse.data;
      setProjects(projectsData);

      // Build stats from dedicated dashboard endpoint (O(1) instead of O(N))
      if (dashboardResponse) {
        const d = dashboardResponse.data;
        setStats({
          totalProjects: projectsData.length,
          totalTasks: d.totalTasks ?? 0,
          completedTasks: d.completedTasks ?? 0,
          inProgressTasks: d.inProgressTasks ?? 0,
        });
      } else {
        // Fallback: compute from my tasks locally if the endpoint failed
        const myTasks = myTasksResponse.data;
        setStats({
          totalProjects: projectsData.length,
          totalTasks: myTasks.length,
          completedTasks: myTasks.filter(t => t.status === 'DONE').length,
          inProgressTasks: myTasks.filter(t => t.status === 'IN_PROGRESS').length,
        });
      }

      // Show 5 most recently created tasks assigned to me
      const sortedTasks = [...myTasksResponse.data].sort(
        (a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0)
      );
      setRecentTasks(sortedTasks.slice(0, 5));

    } catch (error) {
      console.error('Error fetching dashboard data:', error);
      toast.error('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'TODO':
        return <Clock className="h-3 w-3" />;
      case 'IN_PROGRESS':
        return <AlertCircle className="h-3 w-3" />;
      case 'PENDING':
        return <Loader2 className="h-3 w-3" />;
      case 'DONE':
        return <CheckCircle className="h-3 w-3" />;
      default:
        return <Clock className="h-3 w-3" />;
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

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-background">
        <Navbar />
        <div className="container mx-auto px-4 py-8">
          {/* Header */}
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4 }}
            className="flex flex-col md:flex-row md:items-center md:justify-between mb-8"
          >
            <div>
              <h1 className="text-3xl font-bold">Welcome back, {user?.name}!</h1>
              <p className="text-muted-foreground mt-1">
                Here&apos;s what&apos;s happening with your projects today.
              </p>
            </div>
            <div className="mt-4 md:mt-0">
              <Link href="/projects">
                <Button>
                  <Plus className="h-4 w-4 mr-2" />
                  New Project
                </Button>
              </Link>
            </div>
          </motion.div>

          {/* Stats Cards */}
          <motion.div
            initial="hidden"
            animate="visible"
            className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8"
          >
            <motion.div custom={0} variants={cardVariants}>
              <Card className="transition-colors hover:border-primary/40">
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Total Projects</CardTitle>
                  <FolderOpen className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold"><AnimatedNumber value={stats.totalProjects} /></div>
                </CardContent>
              </Card>
            </motion.div>

            <motion.div custom={1} variants={cardVariants}>
              <Card className="transition-colors hover:border-primary/40">
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">My Tasks</CardTitle>
                  <CheckCircle className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold"><AnimatedNumber value={stats.totalTasks} /></div>
                  <p className="text-xs text-muted-foreground mt-1">assigned to me</p>
                </CardContent>
              </Card>
            </motion.div>

            <motion.div custom={2} variants={cardVariants}>
              <Card className="transition-colors hover:border-[#5fd39a]/40">
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Completed</CardTitle>
                  <CheckCircle className="h-4 w-4 text-[#5fd39a]" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-[#5fd39a]"><AnimatedNumber value={stats.completedTasks} /></div>
                </CardContent>
              </Card>
            </motion.div>

            <motion.div custom={3} variants={cardVariants}>
              <Card className="transition-colors hover:border-[#6ea8fe]/40">
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">In Progress</CardTitle>
                  <AlertCircle className="h-4 w-4 text-[#6ea8fe]" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-[#6ea8fe]"><AnimatedNumber value={stats.inProgressTasks} /></div>
                </CardContent>
              </Card>
            </motion.div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.24 }}
            className="grid grid-cols-1 lg:grid-cols-2 gap-8"
          >
            {/* Recent Projects */}
            <Card>
              <CardHeader>
                <CardTitle>Recent Projects</CardTitle>
                <CardDescription>
                  Your most recently created projects
                </CardDescription>
              </CardHeader>
              <CardContent>
                {projects.length === 0 ? (
                  <div className="text-center py-8">
                    <FolderOpen className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                    <p className="text-muted-foreground mb-4">No projects yet</p>
                    <Link href="/projects">
                      <Button>Create your first project</Button>
                    </Link>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {projects.slice(0, 5).map((project) => (
                      <div key={project.id} className="flex items-center justify-between p-3 border rounded-lg transition-colors hover:border-primary/40">
                        <div className="min-w-0 flex-1 mr-2">
                          <h4 className="font-medium truncate">{project.name}</h4>
                          <p className="text-sm text-muted-foreground truncate">{project.description}</p>
                        </div>
                        <Link href={`/projects/${project.id}`}>
                          <Button variant="outline" size="sm">View</Button>
                        </Link>
                      </div>
                    ))}
                    {projects.length > 5 && (
                      <Link href="/projects">
                        <Button variant="ghost" className="w-full">
                          View all {projects.length} projects
                        </Button>
                      </Link>
                    )}
                  </div>
                )}
              </CardContent>
            </Card>

            {/* My Recent Tasks */}
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div>
                    <CardTitle>My Recent Tasks</CardTitle>
                    <CardDescription>
                      Latest tasks assigned to you
                    </CardDescription>
                  </div>
                  <Link href="/my-tasks">
                    <Button variant="outline" size="sm">View All</Button>
                  </Link>
                </div>
              </CardHeader>
              <CardContent>
                {recentTasks.length === 0 ? (
                  <div className="text-center py-8">
                    <CheckCircle className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                    <p className="text-muted-foreground">No tasks assigned to you yet</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {recentTasks.map((task) => (
                      <div key={task.id} className="flex items-center justify-between p-3 border rounded-lg transition-colors hover:border-primary/40">
                        <div className="flex-1 min-w-0 mr-2">
                          <h4 className="font-medium truncate">{task.title}</h4>
                          <p className="text-sm text-muted-foreground truncate">{task.description}</p>
                        </div>
                        <span className={`ml-2 ${statusBadgeClass(task.status)}`}>
                          {getStatusIcon(task.status)}
                          <span>{statusLabel(task.status)}</span>
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </motion.div>
        </div>
      </div>
    </ProtectedRoute>
  );
}
