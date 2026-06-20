/**
 * Tests for src/components/tasks/TaskModal.jsx
 *
 * Critical areas:
 *  - Bug #3: PENDING must appear in status dropdown
 *  - Create vs Edit mode (dialog title changes)
 *  - Validation: submit blocked when title is empty
 *  - onSave is called with correct payload
 *  - Due date minimum is today
 */
import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import TaskModal from '../components/tasks/TaskModal';

// Radix Select fires pointer events; we need to mock it for jsdom
// We use a simple native <select> mock for Select components in tests
jest.mock('../components/ui/select', () => {
  const React = require('react');
  return {
    Select: ({ children, value, onValueChange }) =>
      React.createElement('div', { 'data-testid': 'select-wrapper' }, children),
    SelectTrigger: ({ children }) =>
      React.createElement('button', { type: 'button' }, children),
    SelectValue: ({ placeholder }) =>
      React.createElement('span', null, placeholder),
    SelectContent: ({ children }) =>
      React.createElement('div', { 'data-testid': 'select-content' }, children),
    SelectItem: ({ value, children, onClick }) =>
      React.createElement(
        'option',
        { value, onClick: () => {} },
        children
      ),
  };
});

// Mock Dialog to just render children (avoids Radix portal issues in jsdom)
jest.mock('../components/ui/dialog', () => {
  const React = require('react');
  return {
    Dialog: ({ open, children }) => open ? React.createElement('div', { 'data-testid': 'dialog' }, children) : null,
    DialogContent: ({ children }) => React.createElement('div', null, children),
    DialogHeader: ({ children }) => React.createElement('div', null, children),
    DialogTitle: ({ children }) => React.createElement('h2', null, children),
    DialogDescription: ({ children }) => React.createElement('p', null, children),
    DialogFooter: ({ children }) => React.createElement('div', null, children),
  };
});

// ── Helpers ───────────────────────────────────────────────────────────────────

const renderModal = (props = {}) => {
  const defaults = {
    isOpen: true,
    onClose: jest.fn(),
    onSave: jest.fn(),
    task: null,
    projectMembers: [],
    loading: false,
  };
  return render(<TaskModal {...defaults} {...props} />);
};

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('TaskModal', () => {
  it('renders "Create New Task" title when no task is passed', () => {
    renderModal();
    expect(screen.getByText('Create New Task')).toBeInTheDocument();
  });

  it('renders "Edit Task" title when a task is passed', () => {
    renderModal({
      task: {
        id: 't1',
        title: 'Existing Task',
        description: 'desc',
        status: 'TODO',
        priority: 'MEDIUM',
        assigneeEmail: '',
        dueDate: '',
      },
    });
    expect(screen.getByText('Edit Task')).toBeInTheDocument();
  });

  it('pre-fills the title input when editing a task', () => {
    renderModal({
      task: {
        id: 't1',
        title: 'Existing Task Title',
        description: '',
        status: 'TODO',
        priority: 'LOW',
        assigneeEmail: '',
        dueDate: '',
      },
    });
    expect(screen.getByDisplayValue('Existing Task Title')).toBeInTheDocument();
  });

  // ── Bug #3: PENDING in status dropdown ───────────────────────────────────

  it('includes PENDING as a status option in the dropdown', () => {
    renderModal();
    // All four status options should be present in the rendered options
    expect(screen.getByText('To Do')).toBeInTheDocument();
    expect(screen.getByText('In Progress')).toBeInTheDocument();
    expect(screen.getByText('Pending')).toBeInTheDocument();   // Bug #3
    expect(screen.getByText('Done')).toBeInTheDocument();
  });

  // ── Validation ───────────────────────────────────────────────────────────

  it('does not call onSave when title is empty', () => {
    const onSave = jest.fn();
    renderModal({ onSave });

    // Title input is empty by default
    fireEvent.click(screen.getByText('Create Task'));
    expect(onSave).not.toHaveBeenCalled();
  });

  it('calls onSave with correct payload when form is valid', async () => {
    const onSave = jest.fn();
    renderModal({ onSave });

    fireEvent.change(screen.getByPlaceholderText('Enter task title'), {
      target: { value: 'My New Task' },
    });

    fireEvent.submit(screen.getByText('Create Task').closest('form'));
    // onSave receives the formData object
    await waitFor(() => {
      expect(onSave).toHaveBeenCalledWith(
        expect.objectContaining({ title: 'My New Task' })
      );
    });
  });

  it('shows "Saving..." on the submit button when loading is true', () => {
    renderModal({ loading: true });
    expect(screen.getByText('Saving...')).toBeInTheDocument();
    expect(screen.getByText('Saving...')).toBeDisabled();
  });

  it('shows "Update Task" submit button in edit mode', () => {
    renderModal({
      task: { id: 't1', title: 'Task', description: '', status: 'TODO', priority: 'MEDIUM', assigneeEmail: '', dueDate: '' },
    });
    expect(screen.getByText('Update Task')).toBeInTheDocument();
  });

  it('resets form to empty values when opened in create mode', () => {
    renderModal();
    const titleInput = screen.getByPlaceholderText('Enter task title');
    expect(titleInput.value).toBe('');
  });

  it('renders priority options: Low, Medium, High', () => {
    renderModal();
    expect(screen.getByText('Low')).toBeInTheDocument();
    expect(screen.getByText('Medium')).toBeInTheDocument();
    expect(screen.getByText('High')).toBeInTheDocument();
  });

  it('renders assignee options from projectMembers', () => {
    const members = [
      { id: 'm1', role: 'MEMBER', user: { id: 'u1', name: 'Alice', email: 'alice@example.com' } },
      { id: 'm2', role: 'MANAGER', user: { id: 'u2', name: 'Bob', email: 'bob@example.com' } },
    ];
    renderModal({ projectMembers: members });
    expect(screen.getByText('Alice (MEMBER)')).toBeInTheDocument();
    expect(screen.getByText('Bob (MANAGER)')).toBeInTheDocument();
  });

  it('calls onClose when Cancel button is clicked', () => {
    const onClose = jest.fn();
    renderModal({ onClose });
    fireEvent.click(screen.getByText('Cancel'));
    expect(onClose).toHaveBeenCalled();
  });
});
