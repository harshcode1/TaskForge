/**
 * Tests for src/services/api.js
 *
 * Validates that every API function sends the correct HTTP method,
 * URL, and payload. Uses axios-mock-adapter to intercept calls
 * without hitting a real server.
 */
import MockAdapter from 'axios-mock-adapter';

// Import the named exports AND the default axios instance
import api, {
  authAPI,
  projectsAPI,
  projectMembersAPI,
  tasksAPI,
  commentsAPI,
  dashboardAPI,
} from '../services/api';

// api.js uses axios.create() — we must mock THAT instance, not global axios
const mock = new MockAdapter(api);

beforeEach(() => {
  mock.reset();
  localStorage.clear();
});

afterAll(() => {
  mock.restore();
});

// ─── Auth ────────────────────────────────────────────────────────────────────

describe('authAPI', () => {
  test('login sends POST /auth/login with email and password', async () => {
    mock.onPost('http://localhost:6060/api/auth/login').reply(200, {
      token: 'jwt-token',
      userId: 'u1',
      name: 'Test User',
      email: 'test@example.com',
      role: 'MEMBER',
    });

    const res = await authAPI.login('test@example.com', 'password123');
    expect(res.data.token).toBe('jwt-token');
    expect(mock.history.post[0].url).toBe('/auth/login');
    expect(JSON.parse(mock.history.post[0].data)).toEqual({
      email: 'test@example.com',
      password: 'password123',
    });
  });

  test('register sends POST /auth/register with name, email, password', async () => {
    mock.onPost('http://localhost:6060/api/auth/register').reply(201, {
      token: 'jwt-token',
      userId: 'u2',
      name: 'New User',
      email: 'new@example.com',
      role: 'MEMBER',
    });

    const res = await authAPI.register('New User', 'new@example.com', 'Pass123!');
    expect(res.data.userId).toBe('u2');
    expect(JSON.parse(mock.history.post[0].data)).toEqual({
      name: 'New User',
      email: 'new@example.com',
      password: 'Pass123!',
    });
  });
});

// ─── Projects ────────────────────────────────────────────────────────────────

describe('projectsAPI', () => {
  test('getAll sends GET /projects', async () => {
    mock.onGet('http://localhost:6060/api/projects').reply(200, []);
    const res = await projectsAPI.getAll();
    expect(res.data).toEqual([]);
    expect(mock.history.get[0].url).toBe('/projects');
  });

  test('getById sends GET /projects/{id}', async () => {
    const projectId = 'proj-123';
    mock.onGet(`http://localhost:6060/api/projects/${projectId}`).reply(200, { id: projectId, name: 'Test' });
    const res = await projectsAPI.getById(projectId);
    expect(res.data.id).toBe(projectId);
    expect(mock.history.get[0].url).toBe(`/projects/${projectId}`);
  });

  test('create sends POST /projects with name and description', async () => {
    mock.onPost('http://localhost:6060/api/projects').reply(201, { id: 'new-proj', name: 'New Project' });
    await projectsAPI.create('New Project', 'A description');
    expect(mock.history.post[0].url).toBe('/projects');
    expect(JSON.parse(mock.history.post[0].data)).toEqual({
      name: 'New Project',
      description: 'A description',
    });
  });

  // Bug #1 fix — update was missing
  test('update sends PUT /projects/{id} with name and description', async () => {
    const projectId = 'proj-456';
    mock.onPut(`http://localhost:6060/api/projects/${projectId}`).reply(200, {
      id: projectId,
      name: 'Updated Name',
      description: 'Updated desc',
    });

    const res = await projectsAPI.update(projectId, 'Updated Name', 'Updated desc');
    expect(res.data.name).toBe('Updated Name');
    expect(mock.history.put[0].url).toBe(`/projects/${projectId}`);
    expect(JSON.parse(mock.history.put[0].data)).toEqual({
      name: 'Updated Name',
      description: 'Updated desc',
    });
  });

  test('delete sends DELETE /projects/{id}', async () => {
    const projectId = 'proj-789';
    mock.onDelete(`http://localhost:6060/api/projects/${projectId}`).reply(204);
    await projectsAPI.delete(projectId);
    expect(mock.history.delete[0].url).toBe(`/projects/${projectId}`);
  });
});

// ─── Tasks ───────────────────────────────────────────────────────────────────

describe('tasksAPI', () => {
  test('getByProject sends GET /tasks/project/{id} without status filter', async () => {
    mock.onGet('http://localhost:6060/api/tasks/project/p1').reply(200, []);
    await tasksAPI.getByProject('p1');
    expect(mock.history.get[0].url).toBe('/tasks/project/p1');
    expect(mock.history.get[0].params).toEqual({});
  });

  test('getByProject sends ?status= query param when provided', async () => {
    mock.onGet('http://localhost:6060/api/tasks/project/p1').reply(200, []);
    await tasksAPI.getByProject('p1', 'TODO');
    expect(mock.history.get[0].params).toEqual({ status: 'TODO' });
  });

  test('getMyTasks sends GET /tasks', async () => {
    mock.onGet('http://localhost:6060/api/tasks').reply(200, []);
    await tasksAPI.getMyTasks();
    expect(mock.history.get[0].url).toBe('/tasks');
  });

  test('create sends POST /tasks with all fields', async () => {
    mock.onPost('http://localhost:6060/api/tasks').reply(201, { id: 't1' });
    await tasksAPI.create('p1', 'Title', 'Desc', '2025-12-31', 'TODO', 'HIGH', 'user@example.com');
    const body = JSON.parse(mock.history.post[0].data);
    expect(body.projectId).toBe('p1');
    expect(body.title).toBe('Title');
    expect(body.status).toBe('TODO');
    expect(body.priority).toBe('HIGH');
  });

  test('update sends PUT /tasks/{id} with data', async () => {
    mock.onPut('http://localhost:6060/api/tasks/t1').reply(200, { id: 't1', status: 'DONE' });
    await tasksAPI.update('t1', { status: 'DONE', title: 'Task' });
    expect(mock.history.put[0].url).toBe('/tasks/t1');
    expect(JSON.parse(mock.history.put[0].data)).toMatchObject({ status: 'DONE' });
  });

  test('delete sends DELETE /tasks/{id}', async () => {
    mock.onDelete('http://localhost:6060/api/tasks/t1').reply(204);
    await tasksAPI.delete('t1');
    expect(mock.history.delete[0].url).toBe('/tasks/t1');
  });
});

// ─── Comments ────────────────────────────────────────────────────────────────

describe('commentsAPI', () => {
  test('add sends POST /comments/add with taskId and text', async () => {
    mock.onPost('http://localhost:6060/api/comments/add').reply(201, {
      id: 'c1',
      content: 'Hello',
      createdAt: '2024-01-01T10:00:00',
      author: { id: 'u1', name: 'John', email: 'john@example.com' },
    });
    const res = await commentsAPI.add('task-1', 'Hello');
    // Validate request body
    expect(JSON.parse(mock.history.post[0].data)).toEqual({ taskId: 'task-1', text: 'Hello' });
    // Validate response shape uses `content` not `text`
    expect(res.data.content).toBe('Hello');
    expect(res.data.author.name).toBe('John');
  });

  test('getByTask sends GET /comments/task/{taskId}', async () => {
    mock.onGet('http://localhost:6060/api/comments/task/task-1').reply(200, []);
    await commentsAPI.getByTask('task-1');
    expect(mock.history.get[0].url).toBe('/comments/task/task-1');
  });

  // Bug #2 fix — delete was missing
  test('delete sends DELETE /comments/{id}', async () => {
    mock.onDelete('http://localhost:6060/api/comments/c1').reply(204);
    await commentsAPI.delete('c1');
    expect(mock.history.delete[0].url).toBe('/comments/c1');
  });
});

// ─── Dashboard ───────────────────────────────────────────────────────────────

describe('dashboardAPI', () => {
  test('getProjectSummary sends GET /dashboard/{projectId}', async () => {
    mock.onGet('http://localhost:6060/api/dashboard/p1').reply(200, { totalTasks: 5 });
    const res = await dashboardAPI.getProjectSummary('p1');
    expect(res.data.totalTasks).toBe(5);
    expect(mock.history.get[0].url).toBe('/dashboard/p1');
  });

  // Bug #4 fix — getUserDashboard was missing
  test('getUserDashboard sends GET /dashboard/my-dashboard', async () => {
    mock.onGet('http://localhost:6060/api/dashboard/my-dashboard').reply(200, {
      totalTasks: 10,
      completedTasks: 3,
      pendingTasks: 2,
      inProgressTasks: 5,
      completionRate: 30,
      overdueTasks: 1,
    });
    const res = await dashboardAPI.getUserDashboard();
    expect(res.data.totalTasks).toBe(10);
    expect(mock.history.get[0].url).toBe('/dashboard/my-dashboard');
  });
});

// ─── Auth token injection ─────────────────────────────────────────────────────

describe('request interceptor', () => {
  test('adds Authorization header when token exists in localStorage', async () => {
    localStorage.setItem('token', 'my-jwt-token');
    mock.onGet('http://localhost:6060/api/projects').reply(200, []);
    await projectsAPI.getAll();
    expect(mock.history.get[0].headers.Authorization).toBe('Bearer my-jwt-token');
    localStorage.removeItem('token');
  });

  test('does NOT add Authorization header when no token', async () => {
    localStorage.removeItem('token');
    mock.onGet('http://localhost:6060/api/projects').reply(200, []);
    await projectsAPI.getAll();
    expect(mock.history.get[0].headers.Authorization).toBeUndefined();
  });
});
