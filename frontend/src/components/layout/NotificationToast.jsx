'use client';

import { useEffect, useState } from 'react';
import { toast } from 'react-hot-toast';
import { Bell, CheckCircle, AlertCircle, Info, X } from 'lucide-react';

// Custom notification types
export const NotificationTypes = {
  SUCCESS: 'success',
  ERROR: 'error',
  INFO: 'info',
  WARNING: 'warning',
  TASK_ASSIGNED: 'task_assigned',
  TASK_COMPLETED: 'task_completed',
  PROJECT_INVITE: 'project_invite',
  DEADLINE_REMINDER: 'deadline_reminder',
};

// Notification service
class NotificationService {
  constructor() {
    this.notifications = [];
    this.listeners = [];
  }

  subscribe(listener) {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter(l => l !== listener);
    };
  }

  notify(notification) {
    const id = Date.now().toString();
    const newNotification = {
      id,
      timestamp: new Date(),
      read: false,
      ...notification,
    };

    this.notifications.unshift(newNotification);
    this.listeners.forEach(listener => listener(this.notifications));

    // Show toast notification
    this.showToast(newNotification);

    return id;
  }

  showToast(notification) {
    const { type, title, message } = notification;

    const getIcon = () => {
      switch (type) {
        case NotificationTypes.SUCCESS:
        case NotificationTypes.TASK_COMPLETED:
          return <CheckCircle className="h-4 w-4 text-green-600" />;
        case NotificationTypes.ERROR:
          return <AlertCircle className="h-4 w-4 text-red-600" />;
        case NotificationTypes.WARNING:
        case NotificationTypes.DEADLINE_REMINDER:
          return <AlertCircle className="h-4 w-4 text-yellow-600" />;
        case NotificationTypes.TASK_ASSIGNED:
        case NotificationTypes.PROJECT_INVITE:
          return <Bell className="h-4 w-4 text-blue-600" />;
        default:
          return <Info className="h-4 w-4 text-blue-600" />;
      }
    };

    const getToastType = () => {
      switch (type) {
        case NotificationTypes.SUCCESS:
        case NotificationTypes.TASK_COMPLETED:
          return 'success';
        case NotificationTypes.ERROR:
          return 'error';
        default:
          return 'custom';
      }
    };

    if (getToastType() === 'custom') {
      toast.custom((t) => (
        <div
          className={`${
            t.visible ? 'animate-enter' : 'animate-leave'
          } max-w-md w-full bg-white dark:bg-gray-800 shadow-lg rounded-lg pointer-events-auto flex ring-1 ring-black ring-opacity-5`}
        >
          <div className="flex-1 w-0 p-4">
            <div className="flex items-start">
              <div className="flex-shrink-0">
                {getIcon()}
              </div>
              <div className="ml-3 flex-1">
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                  {title}
                </p>
                {message && (
                  <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                    {message}
                  </p>
                )}
              </div>
            </div>
          </div>
          <div className="flex border-l border-gray-200 dark:border-gray-700">
            <button
              onClick={() => toast.dismiss(t.id)}
              className="w-full border border-transparent rounded-none rounded-r-lg p-4 flex items-center justify-center text-sm font-medium text-gray-600 dark:text-gray-400 hover:text-gray-500 dark:hover:text-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>
      ), {
        duration: 5000,
      });
    } else {
      toast[getToastType()](title + (message ? `: ${message}` : ''));
    }
  }

  markAsRead(id) {
    this.notifications = this.notifications.map(n =>
      n.id === id ? { ...n, read: true } : n
    );
    this.listeners.forEach(listener => listener(this.notifications));
  }

  markAllAsRead() {
    this.notifications = this.notifications.map(n => ({ ...n, read: true }));
    this.listeners.forEach(listener => listener(this.notifications));
  }

  getUnreadCount() {
    return this.notifications.filter(n => !n.read).length;
  }

  getNotifications() {
    return this.notifications;
  }

  clear() {
    this.notifications = [];
    this.listeners.forEach(listener => listener(this.notifications));
  }
}

// Create singleton instance
export const notificationService = new NotificationService();

// Hook for using notifications
export function useNotifications() {
  const [notifications, setNotifications] = useState([]);

  useEffect(() => {
    const unsubscribe = notificationService.subscribe(setNotifications);
    setNotifications(notificationService.getNotifications());
    return unsubscribe;
  }, []);

  return {
    notifications,
    unreadCount: notificationService.getUnreadCount(),
    markAsRead: notificationService.markAsRead.bind(notificationService),
    markAllAsRead: notificationService.markAllAsRead.bind(notificationService),
    clear: notificationService.clear.bind(notificationService),
  };
}

// Helper functions for common notifications
export const showNotification = {
  success: (title, message) => notificationService.notify({
    type: NotificationTypes.SUCCESS,
    title,
    message,
  }),

  error: (title, message) => notificationService.notify({
    type: NotificationTypes.ERROR,
    title,
    message,
  }),

  info: (title, message) => notificationService.notify({
    type: NotificationTypes.INFO,
    title,
    message,
  }),

  warning: (title, message) => notificationService.notify({
    type: NotificationTypes.WARNING,
    title,
    message,
  }),

  taskAssigned: (taskTitle, assignee) => notificationService.notify({
    type: NotificationTypes.TASK_ASSIGNED,
    title: 'Task Assigned',
    message: `"${taskTitle}" has been assigned to ${assignee}`,
  }),

  taskCompleted: (taskTitle) => notificationService.notify({
    type: NotificationTypes.TASK_COMPLETED,
    title: 'Task Completed',
    message: `"${taskTitle}" has been marked as completed`,
  }),

  projectInvite: (projectName, inviter) => notificationService.notify({
    type: NotificationTypes.PROJECT_INVITE,
    title: 'Project Invitation',
    message: `${inviter} invited you to join "${projectName}"`,
  }),

  deadlineReminder: (taskTitle, dueDate) => notificationService.notify({
    type: NotificationTypes.DEADLINE_REMINDER,
    title: 'Deadline Reminder',
    message: `"${taskTitle}" is due ${dueDate}`,
  }),
};

export default NotificationService;

