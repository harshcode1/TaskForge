package com.projectmgmttool.backend.dto;

import java.util.Map;

public class DashboardResponse {
    private Map<String, Long> taskStatusCounts;
    private Map<String, Long> tasksPerUser;
    private long totalTasks;
    private long completedTasks;
    private long pendingTasks;
    private long inProgressTasks;
    private int completionRate;
    private long overdueTasks;
    private int totalMembers;

    public DashboardResponse() {}

    public DashboardResponse(Map<String, Long> taskStatusCounts, Map<String, Long> tasksPerUser) {
        this.taskStatusCounts = taskStatusCounts;
        this.tasksPerUser = tasksPerUser;
    }

    public Map<String, Long> getTaskStatusCounts() { return taskStatusCounts; }
    public void setTaskStatusCounts(Map<String, Long> taskStatusCounts) { this.taskStatusCounts = taskStatusCounts; }
    public Map<String, Long> getTasksPerUser() { return tasksPerUser; }
    public void setTasksPerUser(Map<String, Long> tasksPerUser) { this.tasksPerUser = tasksPerUser; }
    public long getTotalTasks() { return totalTasks; }
    public void setTotalTasks(long totalTasks) { this.totalTasks = totalTasks; }
    public long getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(long completedTasks) { this.completedTasks = completedTasks; }
    public long getPendingTasks() { return pendingTasks; }
    public void setPendingTasks(long pendingTasks) { this.pendingTasks = pendingTasks; }
    public long getInProgressTasks() { return inProgressTasks; }
    public void setInProgressTasks(long inProgressTasks) { this.inProgressTasks = inProgressTasks; }
    public int getCompletionRate() { return completionRate; }
    public void setCompletionRate(int completionRate) { this.completionRate = completionRate; }
    public long getOverdueTasks() { return overdueTasks; }
    public void setOverdueTasks(long overdueTasks) { this.overdueTasks = overdueTasks; }
    public int getTotalMembers() { return totalMembers; }
    public void setTotalMembers(int totalMembers) { this.totalMembers = totalMembers; }
}
