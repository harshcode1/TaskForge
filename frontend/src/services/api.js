import axios from 'axios';

const API_BASE_URL = 'http://localhost:6060/api';

// Create axios instance
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  login: (email, password) => api.post('/auth/login', { email, password }),
  register: (name, email, password) => api.post('/auth/register', { name, email, password }),
};

// Projects API
export const projectsAPI = {
  create: (name, description) => api.post('/projects', { name, description }),
  getAll: () => api.get('/projects'),
  getById: (id) => api.get(`/projects/${id}`),
  update: (id, name, description) => api.put(`/projects/${id}`, { name, description }),
  delete: (id) => api.delete(`/projects/${id}`),
};

// Project Members API
export const projectMembersAPI = {
  invite: (projectId, userEmail, role) => api.post('/project-members/invite', { projectId, userEmail, role }),
  getMembers: (projectId) => api.get(`/project-members/${projectId}`),
};

// Tasks API
export const tasksAPI = {
  create: (projectId, title, description, dueDate, status, priority, assigneeEmail) =>
    api.post('/tasks', { projectId, title, description, dueDate, status, priority, assigneeEmail }),
  update: (id, data) => api.put(`/tasks/${id}`, data),
  getMyTasks: () => api.get('/tasks'),
  getByProject: (projectId, status) => {
    const params = status ? { status } : {};
    return api.get(`/tasks/project/${projectId}`, { params });
  },
  delete: (id) => api.delete(`/tasks/${id}`),
};

// Comments API
export const commentsAPI = {
  add: (taskId, text) => api.post('/comments/add', { taskId, text }),
  getByTask: (taskId) => api.get(`/comments/task/${taskId}`),
  delete: (id) => api.delete(`/comments/${id}`),
};

// Dashboard API
export const dashboardAPI = {
  getProjectSummary: (projectId) => api.get(`/dashboard/${projectId}`),
  getUserDashboard: () => api.get('/dashboard/my-dashboard'),
};

export default api;

