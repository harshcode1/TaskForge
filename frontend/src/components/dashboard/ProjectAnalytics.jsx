'use client';

import { useState, useEffect, useCallback } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Progress } from '@/components/ui/progress';
import { 
  BarChart, 
  Bar, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line,
  Area,
  AreaChart,
  Legend
} from 'recharts';
import { dashboardAPI } from '@/services/api';
import { toast } from 'react-hot-toast';
import { TrendingUp, TrendingDown, Clock, CheckCircle, AlertCircle, Users } from 'lucide-react';
import { CHART_COLORS, PRIORITY_CHART_COLORS, STATUS_CHART_COLORS } from '@/lib/task-ui';

// Order matches getTaskStatusData()'s fixed key order (To Do / In Progress / Done)
const STATUS_PIE_COLORS = [STATUS_CHART_COLORS.TODO, STATUS_CHART_COLORS.IN_PROGRESS, STATUS_CHART_COLORS.DONE];
// Order matches getPriorityData()'s fixed key order (High / Medium / Low)
const PRIORITY_BAR_COLORS = [PRIORITY_CHART_COLORS.HIGH, PRIORITY_CHART_COLORS.MEDIUM, PRIORITY_CHART_COLORS.LOW];
const ASSIGNEE_BAR_COLOR = CHART_COLORS.amber;

export default function ProjectAnalytics({ projectId, tasks = [], members = [] }) {
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(false);

  const generateLocalAnalytics = useCallback(() => {
    const totalTasks = tasks.length;
    const completedTasks = tasks.filter(t => t.status === 'DONE').length;
    const inProgressTasks = tasks.filter(t => t.status === 'IN_PROGRESS').length;
    const todoTasks = tasks.filter(t => t.status === 'TODO').length;

    setAnalytics({
      totalTasks,
      completedTasks,
      pendingTasks: totalTasks - completedTasks,
      completionRate: totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0,
    });
  }, [tasks]);

  const fetchAnalytics = useCallback(async () => {
    setLoading(true);
    try {
      const response = await dashboardAPI.getProjectSummary(projectId);
      setAnalytics(response.data);
    } catch (error) {
      console.error('Error fetching analytics:', error);
      // Use local data if API fails
      generateLocalAnalytics();
    } finally {
      setLoading(false);
    }
  }, [projectId, generateLocalAnalytics]);

  useEffect(() => {
    if (projectId) {
      fetchAnalytics();
    }
  }, [projectId, fetchAnalytics]);

  const getTaskStatusData = () => {
    const statusCounts = {
      'To Do': tasks.filter(t => t.status === 'TODO').length,
      'In Progress': tasks.filter(t => t.status === 'IN_PROGRESS').length,
      'Done': tasks.filter(t => t.status === 'DONE').length,
    };

    return Object.entries(statusCounts).map(([name, value]) => ({
      name,
      value,
    }));
  };

  const getPriorityData = () => {
    const priorityCounts = {
      'High': tasks.filter(t => t.priority === 'HIGH').length,
      'Medium': tasks.filter(t => t.priority === 'MEDIUM').length,
      'Low': tasks.filter(t => t.priority === 'LOW').length,
    };

    return Object.entries(priorityCounts).map(([name, value]) => ({
      name,
      value,
    }));
  };

  const getAssigneeData = () => {
    const assigneeCounts = {};
    tasks.forEach(task => {
      const assignee = task.assigneeEmail || 'Unassigned';
      assigneeCounts[assignee] = (assigneeCounts[assignee] || 0) + 1;
    });

    return Object.entries(assigneeCounts)
      .map(([name, value]) => ({
        name: name.length > 20 ? name.substring(0, 20) + '...' : name,
        value,
      }))
      .sort((a, b) => b.value - a.value)
      .slice(0, 5);
  };

  const getOverdueTasks = () => {
    const now = new Date();
    return tasks.filter(task => 
      task.dueDate && 
      new Date(task.dueDate) < now && 
      task.status !== 'DONE'
    );
  };

  const getUpcomingTasks = () => {
    const now = new Date();
    const nextWeek = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
    
    return tasks.filter(task => 
      task.dueDate && 
      new Date(task.dueDate) >= now && 
      new Date(task.dueDate) <= nextWeek &&
      task.status !== 'DONE'
    );
  };

  const getCompletionTrend = () => {
    // Tasks created per day over the last 7 days, from real task.createdAt
    // values. There's deliberately no "completed" series here: the Task
    // entity only has createdAt, no completedAt/updatedAt, so a completion
    // trend can't actually be computed from the data we have — the previous
    // version of this chart filled that gap with Math.random(), which is
    // worse than not showing it at all.
    const days = [];
    for (let i = 6; i >= 0; i--) {
      const date = new Date();
      date.setDate(date.getDate() - i);
      const dayKey = date.toDateString();

      const created = tasks.filter(t => t.createdAt && new Date(t.createdAt).toDateString() === dayKey).length;

      days.push({
        date: date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
        created,
      });
    }
    return days;
  };

  const statusData = getTaskStatusData();
  // Recharts v3's Pie mis-computes sector angles when multiple entries are
  // exactly 0 (confirmed by inspecting the rendered <path>: the one non-zero
  // slice got a ~0.04° sweep instead of 360°) — zero-value pie slices should
  // occupy no angle, not eat the real one's. Pre-filtering to only positive
  // values sidesteps the bug and is also better UX (no empty legend rows).
  const statusPieData = statusData
    .map((entry, index) => ({ ...entry, color: STATUS_PIE_COLORS[index % STATUS_PIE_COLORS.length] }))
    .filter((entry) => entry.value > 0);
  const priorityData = getPriorityData();
  const assigneeData = getAssigneeData();
  const overdueTasks = getOverdueTasks();
  const upcomingTasks = getUpcomingTasks();
  const trendData = getCompletionTrend();

  // Recharts v3's ResponsiveContainer measures its parent via ResizeObserver on
  // first mount. This component only mounts once the (lazily-loaded) Analytics
  // tab is first opened — at that exact moment the outer tab panel is still
  // mid-transition/swapping in from its loading fallback, so ResponsiveContainer
  // takes its very first measurement against a container that hasn't settled
  // into its final layout yet and gets 0 width, rendering nothing. Nothing
  // triggers a re-measure afterward (ResizeObserver only fires on further size
  // *changes*, and none occur), so the chart stays blank permanently.
  // Fix: don't mount the charts until one paint cycle after this component
  // itself has mounted, by which point the surrounding layout is stable.
  const [chartsReady, setChartsReady] = useState(false);
  useEffect(() => {
    const id = requestAnimationFrame(() => requestAnimationFrame(() => setChartsReady(true)));
    return () => cancelAnimationFrame(id);
  }, []);

  const completionRate = tasks.length > 0 
    ? Math.round((tasks.filter(t => t.status === 'DONE').length / tasks.length) * 100)
    : 0;

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Completion Rate</CardTitle>
            <CheckCircle className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{completionRate}%</div>
            <Progress value={completionRate} className="mt-2" />
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Overdue Tasks</CardTitle>
            <AlertCircle className="h-4 w-4 text-destructive" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-destructive">{overdueTasks.length}</div>
            <p className="text-xs text-muted-foreground">
              Need immediate attention
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Due This Week</CardTitle>
            <Clock className="h-4 w-4 text-primary" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-primary">{upcomingTasks.length}</div>
            <p className="text-xs text-muted-foreground">
              Upcoming deadlines
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Team Members</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{members.length}</div>
            <p className="text-xs text-muted-foreground">
              Active contributors
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Charts — gated on chartsReady, see the comment above its definition */}
      {!chartsReady ? (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {[0, 1].map((i) => (
            <Card key={i}>
              <CardContent className="flex h-[356px] items-center justify-center pt-6">
                <div className="h-8 w-8 animate-spin rounded-full border-2 border-muted-foreground/30 border-t-muted-foreground" />
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
      <Tabs defaultValue="status" className="space-y-4">
        <TabsList>
          <TabsTrigger value="status">Task Status</TabsTrigger>
          <TabsTrigger value="priority">Priority</TabsTrigger>
          <TabsTrigger value="assignee">Assignees</TabsTrigger>
          <TabsTrigger value="trend">Trends</TabsTrigger>
        </TabsList>

        <TabsContent value="status" className="space-y-4">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Task Status Distribution</CardTitle>
                <CardDescription>
                  Overview of task completion status
                </CardDescription>
              </CardHeader>
              <CardContent>
                {statusPieData.length === 0 ? (
                  <div className="flex h-[300px] items-center justify-center text-sm text-muted-foreground">
                    No tasks yet
                  </div>
                ) : (
                  <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                      <Pie
                        data={statusPieData}
                        cx="50%"
                        cy="40%"
                        outerRadius={80}
                        isAnimationActive={false}
                        fill={CHART_COLORS.amber}
                        dataKey="value"
                      >
                        {statusPieData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip formatter={(value, name) => [value, name]} />
                      <Legend
                        verticalAlign="bottom"
                        height={36}
                        formatter={(value, entry) => `${value}: ${entry.payload.value}`}
                      />
                    </PieChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Status Breakdown</CardTitle>
                <CardDescription>
                  Detailed task counts by status
                </CardDescription>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={statusData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis />
                    <Tooltip />
                    <Bar dataKey="value" fill={CHART_COLORS.amber} isAnimationActive={false}>
                      {statusData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={STATUS_PIE_COLORS[index % STATUS_PIE_COLORS.length]} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="priority" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Task Priority Distribution</CardTitle>
              <CardDescription>
                Tasks organized by priority level
              </CardDescription>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={priorityData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="value" fill={CHART_COLORS.amber} isAnimationActive={false}>
                    {priorityData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={PRIORITY_BAR_COLORS[index % PRIORITY_BAR_COLORS.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="assignee" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Task Distribution by Assignee</CardTitle>
              <CardDescription>
                Workload distribution across team members
              </CardDescription>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={assigneeData} layout="horizontal">
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis type="number" />
                  <YAxis dataKey="name" type="category" width={100} />
                  <Tooltip />
                  <Bar dataKey="value" fill={ASSIGNEE_BAR_COLOR} isAnimationActive={false} />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="trend" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Tasks Created</CardTitle>
              <CardDescription>
                Daily task creation over the last week
              </CardDescription>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={trendData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Area
                    type="monotone"
                    dataKey="created"
                    isAnimationActive={false}
                    stroke={CHART_COLORS.amber}
                    fill={CHART_COLORS.amber}
                    fillOpacity={0.25}
                    name="Created"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
      )}

      {/* Insights */}
      <Card>
        <CardHeader>
          <CardTitle>Project Insights</CardTitle>
          <CardDescription>
            Key observations and recommendations
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {overdueTasks.length > 0 && (
              <div className="flex items-start space-x-3 rounded-lg border border-destructive/30 bg-destructive/10 p-3">
                <AlertCircle className="h-5 w-5 text-destructive mt-0.5" />
                <div>
                  <p className="font-medium text-destructive">
                    {overdueTasks.length} overdue task{overdueTasks.length > 1 ? 's' : ''}
                  </p>
                  <p className="text-sm text-destructive/80">
                    Consider reassigning or extending deadlines for overdue tasks.
                  </p>
                </div>
              </div>
            )}

            {completionRate >= 80 && (
              <div className="flex items-start space-x-3 rounded-lg border border-[#5fd39a]/30 bg-[#5fd39a]/10 p-3">
                <CheckCircle className="h-5 w-5 text-[#5fd39a] mt-0.5" />
                <div>
                  <p className="font-medium text-[#5fd39a]">
                    Great progress! {completionRate}% completion rate
                  </p>
                  <p className="text-sm text-[#5fd39a]/80">
                    The project is on track with excellent completion rate.
                  </p>
                </div>
              </div>
            )}

            {upcomingTasks.length > 0 && (
              <div className="flex items-start space-x-3 rounded-lg border border-primary/30 bg-primary/10 p-3">
                <Clock className="h-5 w-5 text-primary mt-0.5" />
                <div>
                  <p className="font-medium text-primary">
                    {upcomingTasks.length} task{upcomingTasks.length > 1 ? 's' : ''} due this week
                  </p>
                  <p className="text-sm text-primary/80">
                    Plan ahead to meet upcoming deadlines.
                  </p>
                </div>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

