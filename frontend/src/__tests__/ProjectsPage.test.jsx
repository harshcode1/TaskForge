/**
 * Tests for src/app/projects/page.js
 *
 * Critical areas:
 *  - Bug #1: Edit project dialog opens pre-filled with current data
 *  - Bug #1: handleUpdateProject calls projectsAPI.update with correct args
 *  - Bug #1: project list updates after successful edit (no page reload)
 *  - Bug #1: validation: empty name blocks the API call
 *  - Bug #1: 403 from backend shows correct error toast
 */
import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import ProjectsPage from '../app/projects/page';
import { projectsAPI } from '../services/api';

// ── Mocks ─────────────────────────────────────────────────────────────────────

jest.mock('../services/api', () => ({
  projectsAPI: {
    getAll: jest.fn(),
    create: jest.fn(),
    update: jest.fn(),
    delete: jest.fn(),
  },
}));

jest.mock('../components/auth/ProtectedRoute', () => {
  return function MockProtectedRoute({ children }) { return <>{children}</>; };
});

jest.mock('../components/layout/Navbar', () => {
  return function MockNavbar() { return <nav data-testid="navbar" />; };
});

// ── Test data ─────────────────────────────────────────────────────────────────

const mockProjects = [
  { id: 'p1', name: 'Project Alpha', description: 'First project', createdAt: '2024-01-01T00:00:00', owner: { id: 'u1', name: 'Owner', email: 'owner@example.com' } },
  { id: 'p2', name: 'Project Beta', description: 'Second project', createdAt: '2024-01-02T00:00:00', owner: { id: 'u1', name: 'Owner', email: 'owner@example.com' } },
];

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('ProjectsPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    projectsAPI.getAll.mockResolvedValue({ data: mockProjects });
  });

  it('renders the project list after loading', async () => {
    render(<ProjectsPage />);
    await waitFor(() => {
      expect(screen.getByText('Project Alpha')).toBeInTheDocument();
      expect(screen.getByText('Project Beta')).toBeInTheDocument();
    });
  });

  // ── Bug #1: Edit button exists ────────────────────────────────────────────

  it('renders an Edit (pencil) button for each project card', async () => {
    render(<ProjectsPage />);
    await waitFor(() => {
      const editButtons = screen.getAllByTitle('Edit project');
      expect(editButtons).toHaveLength(2); // one per project
    });
  });

  // ── Bug #1: Edit dialog opens pre-filled ─────────────────────────────────

  it('opens the edit dialog pre-filled with the project name when Edit is clicked', async () => {
    render(<ProjectsPage />);
    await waitFor(() => screen.getAllByTitle('Edit project'));

    const editButtons = screen.getAllByTitle('Edit project');
    fireEvent.click(editButtons[0]); // click edit on "Project Alpha"

    await waitFor(() => {
      // The edit dialog input should be pre-filled with "Project Alpha"
      const nameInput = screen.getByDisplayValue('Project Alpha');
      expect(nameInput).toBeInTheDocument();
    });
  });

  // ── Bug #1: API call with correct args ───────────────────────────────────

  it('calls projectsAPI.update with the correct id and data on save', async () => {
    projectsAPI.update.mockResolvedValue({
      data: { id: 'p1', name: 'Updated Alpha', description: 'New desc', createdAt: '2024-01-01T00:00:00', owner: { id: 'u1', name: 'Owner', email: 'owner@example.com' } },
    });

    render(<ProjectsPage />);
    await waitFor(() => screen.getAllByTitle('Edit project'));

    fireEvent.click(screen.getAllByTitle('Edit project')[0]);

    await waitFor(() => screen.getByDisplayValue('Project Alpha'));

    // Change the name
    const nameInput = screen.getByDisplayValue('Project Alpha');
    fireEvent.change(nameInput, { target: { value: 'Updated Alpha' } });

    fireEvent.click(screen.getByText('Save Changes'));

    await waitFor(() => {
      expect(projectsAPI.update).toHaveBeenCalledWith('p1', 'Updated Alpha', 'First project');
    });
  });

  // ── Bug #1: local state updates after edit ───────────────────────────────

  it('updates the project name in the list without page reload after successful edit', async () => {
    projectsAPI.update.mockResolvedValue({
      data: { id: 'p1', name: 'Updated Alpha', description: 'New desc', createdAt: '2024-01-01T00:00:00', owner: { id: 'u1', name: 'Owner', email: 'owner@example.com' } },
    });

    render(<ProjectsPage />);
    await waitFor(() => screen.getAllByTitle('Edit project'));

    fireEvent.click(screen.getAllByTitle('Edit project')[0]);
    await waitFor(() => screen.getByDisplayValue('Project Alpha'));

    fireEvent.change(screen.getByDisplayValue('Project Alpha'), {
      target: { value: 'Updated Alpha' },
    });
    fireEvent.click(screen.getByText('Save Changes'));

    await waitFor(() => {
      expect(screen.getByText('Updated Alpha')).toBeInTheDocument();
      expect(screen.queryByText('Project Alpha')).not.toBeInTheDocument();
    });
  });

  // ── Bug #1: validation ───────────────────────────────────────────────────

  it('does NOT call projectsAPI.update when name is blank', async () => {
    render(<ProjectsPage />);
    await waitFor(() => screen.getAllByTitle('Edit project'));

    fireEvent.click(screen.getAllByTitle('Edit project')[0]);
    await waitFor(() => screen.getByDisplayValue('Project Alpha'));

    // Clear the name
    fireEvent.change(screen.getByDisplayValue('Project Alpha'), {
      target: { value: '' },
    });
    fireEvent.click(screen.getByText('Save Changes'));

    expect(projectsAPI.update).not.toHaveBeenCalled();
  });

  // ── Bug #1: 403 error handling ───────────────────────────────────────────

  it('shows "Only the project owner can edit this project" on 403 error', async () => {
    const { toast } = require('react-hot-toast');
    projectsAPI.update.mockRejectedValue({ response: { status: 403 } });

    render(<ProjectsPage />);
    await waitFor(() => screen.getAllByTitle('Edit project'));

    fireEvent.click(screen.getAllByTitle('Edit project')[0]);
    await waitFor(() => screen.getByDisplayValue('Project Alpha'));

    fireEvent.click(screen.getByText('Save Changes'));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Only the project owner can edit this project');
    });
  });
});
