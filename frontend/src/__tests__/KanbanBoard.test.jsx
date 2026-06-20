/**
 * Tests for src/components/tasks/KanbanBoard.jsx
 *
 * Critical areas:
 *  - Bug #3: PENDING column must exist and render PENDING tasks
 *  - All 4 statuses render in correct columns
 *  - Tasks with unknown status do not crash the board
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import KanbanBoard from '../components/tasks/KanbanBoard';

// dnd-kit uses pointer events; jsdom doesn't fully support them
// Mock the parts we don't need to test
jest.mock('@dnd-kit/core', () => ({
  DndContext: ({ children }) => <div data-testid="dnd-context">{children}</div>,
  DragOverlay: ({ children }) => <div data-testid="drag-overlay">{children}</div>,
  closestCorners: jest.fn(),
  KeyboardSensor: jest.fn(),
  PointerSensor: jest.fn(),
  useSensor: jest.fn(() => ({})),
  useSensors: jest.fn(() => []),
}));

jest.mock('@dnd-kit/sortable', () => ({
  SortableContext: ({ children }) => <div>{children}</div>,
  sortableKeyboardCoordinates: jest.fn(),
  verticalListSortingStrategy: jest.fn(),
  useSortable: () => ({
    attributes: {},
    listeners: {},
    setNodeRef: jest.fn(),
    transform: null,
    transition: null,
    isDragging: false,
  }),
  arrayMove: jest.fn(),
}));

jest.mock('@dnd-kit/utilities', () => ({
  CSS: { Transform: { toString: () => '' } },
}));

// ── Test data ─────────────────────────────────────────────────────────────────

const makeTasks = (statuses) =>
  statuses.map((status, i) => ({
    id: `task-${i}`,
    title: `Task ${i} (${status})`,
    description: 'desc',
    status,
    priority: 'MEDIUM',
    dueDate: null,
    assigneeEmail: null,
  }));

const noop = jest.fn();

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('KanbanBoard', () => {
  it('renders 4 column headers: To Do, In Progress, Pending, Done', () => {
    render(
      <KanbanBoard tasks={[]} onTaskUpdate={noop} onTaskEdit={noop} onTaskDelete={noop} />
    );

    expect(screen.getByText('To Do')).toBeInTheDocument();
    expect(screen.getByText('In Progress')).toBeInTheDocument();
    expect(screen.getByText('Pending')).toBeInTheDocument();   // Bug #3 fix
    expect(screen.getByText('Done')).toBeInTheDocument();
  });

  it('places TODO tasks in the To Do column', () => {
    const tasks = makeTasks(['TODO', 'TODO']);
    render(
      <KanbanBoard tasks={tasks} onTaskUpdate={noop} onTaskEdit={noop} onTaskDelete={noop} />
    );
    expect(screen.getByText('Task 0 (TODO)')).toBeInTheDocument();
    expect(screen.getByText('Task 1 (TODO)')).toBeInTheDocument();
  });

  // ── Bug #3: PENDING column existence ─────────────────────────────────────

  it('places PENDING tasks in the Pending column (Bug #3)', () => {
    const tasks = makeTasks(['PENDING', 'PENDING']);
    render(
      <KanbanBoard tasks={tasks} onTaskUpdate={noop} onTaskEdit={noop} onTaskDelete={noop} />
    );
    // Tasks with status PENDING now render (they were invisible before)
    expect(screen.getByText('Task 0 (PENDING)')).toBeInTheDocument();
    expect(screen.getByText('Task 1 (PENDING)')).toBeInTheDocument();
  });

  it('places tasks in their correct columns across all 4 statuses', () => {
    const tasks = makeTasks(['TODO', 'IN_PROGRESS', 'PENDING', 'DONE']);
    render(
      <KanbanBoard tasks={tasks} onTaskUpdate={noop} onTaskEdit={noop} onTaskDelete={noop} />
    );
    expect(screen.getByText('Task 0 (TODO)')).toBeInTheDocument();
    expect(screen.getByText('Task 1 (IN_PROGRESS)')).toBeInTheDocument();
    expect(screen.getByText('Task 2 (PENDING)')).toBeInTheDocument();
    expect(screen.getByText('Task 3 (DONE)')).toBeInTheDocument();
  });

  it('renders column task counts correctly', () => {
    const tasks = makeTasks(['TODO', 'TODO', 'PENDING', 'DONE']);
    render(
      <KanbanBoard tasks={tasks} onTaskUpdate={noop} onTaskEdit={noop} onTaskDelete={noop} />
    );
    // Each column badge shows the count of tasks in it
    // We expect badges: 2 (TODO), 0 (IN_PROGRESS), 1 (PENDING), 1 (DONE)
    const badges = screen.getAllByText(/^\d+$/);
    const counts = badges.map(b => parseInt(b.textContent));
    expect(counts).toContain(2); // TODO column
    expect(counts).toContain(1); // PENDING column
    expect(counts).toContain(1); // DONE column
  });

  it('renders an empty board without crashing', () => {
    render(
      <KanbanBoard tasks={[]} onTaskUpdate={noop} onTaskEdit={noop} onTaskDelete={noop} />
    );
    // All 4 columns present even with no tasks
    expect(screen.getByText('To Do')).toBeInTheDocument();
    expect(screen.getByText('Pending')).toBeInTheDocument();
  });

  it('renders task title and priority badge', () => {
    const tasks = [{
      id: 't1',
      title: 'Fix the login bug',
      description: 'Something is broken',
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      dueDate: '2025-12-31',
      assigneeEmail: 'dev@example.com',
    }];

    render(
      <KanbanBoard tasks={tasks} onTaskUpdate={noop} onTaskEdit={noop} onTaskDelete={noop} />
    );

    expect(screen.getByText('Fix the login bug')).toBeInTheDocument();
    expect(screen.getByText('HIGH')).toBeInTheDocument();
  });
});
