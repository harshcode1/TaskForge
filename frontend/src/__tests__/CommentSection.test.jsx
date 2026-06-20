/**
 * Tests for src/components/tasks/CommentSection.jsx
 *
 * Critical areas:
 *  - Bug #2a: comment.content (not .text) renders as the body
 *  - Bug #2b: comment.author.name (not .authorName) shows in UI
 *  - Bug #2c: delete button visible only to comment author
 *  - Bug #2d: deleting a comment removes it from the list
 */
import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import CommentSection from '../components/tasks/CommentSection';
import { commentsAPI } from '../services/api';

// ── Mock dependencies ─────────────────────────────────────────────────────────

jest.mock('../services/api', () => ({
  commentsAPI: {
    getByTask: jest.fn(),
    add: jest.fn(),
    delete: jest.fn(),
  },
}));

jest.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({
    user: { name: 'Current User', email: 'current@example.com' },
  }),
}));

// ── Test data (matches the real backend Comment entity shape) ─────────────────

const mockComments = [
  {
    id: 'c1',
    content: 'This is the first comment',      // ← field is `content`, NOT `text`
    createdAt: new Date().toISOString(),
    author: {                                   // ← nested object, NOT flat fields
      id: 'u1',
      name: 'Current User',
      email: 'current@example.com',
    },
  },
  {
    id: 'c2',
    content: 'This is a comment by someone else',
    createdAt: new Date().toISOString(),
    author: {
      id: 'u2',
      name: 'Other User',
      email: 'other@example.com',
    },
  },
];

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('CommentSection', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    commentsAPI.getByTask.mockResolvedValue({ data: mockComments });
  });

  it('renders nothing when isOpen is false', () => {
    const { container } = render(
      <CommentSection taskId="task-1" isOpen={false} onClose={jest.fn()} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('fetches and displays comments when isOpen is true', async () => {
    render(<CommentSection taskId="task-1" isOpen={true} onClose={jest.fn()} />);

    await waitFor(() => {
      expect(commentsAPI.getByTask).toHaveBeenCalledWith('task-1');
    });
  });

  // ── Bug #2a: field name fix ────────────────────────────────────────────────

  it('renders comment.content (not comment.text) as the comment body', async () => {
    render(<CommentSection taskId="task-1" isOpen={true} onClose={jest.fn()} />);

    await waitFor(() => {
      expect(screen.getByText('This is the first comment')).toBeInTheDocument();
      expect(screen.getByText('This is a comment by someone else')).toBeInTheDocument();
    });
  });

  // ── Bug #2b: author name fix ──────────────────────────────────────────────

  it('renders comment.author.name (not comment.authorName) as the author', async () => {
    render(<CommentSection taskId="task-1" isOpen={true} onClose={jest.fn()} />);

    await waitFor(() => {
      expect(screen.getByText('Current User')).toBeInTheDocument();
      expect(screen.getByText('Other User')).toBeInTheDocument();
    });
  });

  it('shows "Unknown" when author name is missing', async () => {
    commentsAPI.getByTask.mockResolvedValue({
      data: [{
        id: 'c3',
        content: 'Orphan comment',
        createdAt: new Date().toISOString(),
        author: {},
      }],
    });

    render(<CommentSection taskId="task-1" isOpen={true} onClose={jest.fn()} />);

    await waitFor(() => {
      expect(screen.getByText('Unknown')).toBeInTheDocument();
    });
  });

  // ── Bug #2c: delete button visibility ─────────────────────────────────────

  it('shows delete button only for comments authored by the current user', async () => {
    render(<CommentSection taskId="task-1" isOpen={true} onClose={jest.fn()} />);

    await waitFor(() => {
      // c1 is by current@example.com → delete button should be present
      // c2 is by other@example.com → no delete button for current user
      const trashButtons = screen.getAllByTitle('Delete comment');
      expect(trashButtons).toHaveLength(1); // only 1 delete button for own comment
    });
  });

  // ── Bug #2d: delete removes from list ─────────────────────────────────────

  it('removes a comment from the list after successful delete', async () => {
    commentsAPI.delete.mockResolvedValue({});

    render(<CommentSection taskId="task-1" isOpen={true} onClose={jest.fn()} />);

    await waitFor(() => {
      expect(screen.getByText('This is the first comment')).toBeInTheDocument();
    });

    const deleteBtn = screen.getByTitle('Delete comment');
    fireEvent.click(deleteBtn);

    await waitFor(() => {
      expect(commentsAPI.delete).toHaveBeenCalledWith('c1');
      expect(screen.queryByText('This is the first comment')).not.toBeInTheDocument();
    });
  });

  it('shows error toast when delete fails with 403', async () => {
    const { toast } = require('react-hot-toast');
    commentsAPI.delete.mockRejectedValue({
      response: { status: 403 },
    });

    render(<CommentSection taskId="task-1" isOpen={true} onClose={jest.fn()} />);

    await waitFor(() => screen.getByTitle('Delete comment'));
    fireEvent.click(screen.getByTitle('Delete comment'));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('You can only delete your own comments');
    });
  });

  // ── Add comment ───────────────────────────────────────────────────────────

  it('adds a new comment and appends it to the list', async () => {
    const newComment = {
      id: 'c-new',
      content: 'A brand new comment',
      createdAt: new Date().toISOString(),
      author: { id: 'u1', name: 'Current User', email: 'current@example.com' },
    };
    commentsAPI.add.mockResolvedValue({ data: newComment });

    render(<CommentSection taskId="task-1" isOpen={true} onClose={jest.fn()} />);

    await waitFor(() => screen.getByPlaceholderText('Add a comment...'));

    fireEvent.change(screen.getByPlaceholderText('Add a comment...'), {
      target: { value: 'A brand new comment' },
    });
    fireEvent.click(screen.getByText('Post Comment'));

    await waitFor(() => {
      expect(commentsAPI.add).toHaveBeenCalledWith('task-1', 'A brand new comment');
      expect(screen.getByText('A brand new comment')).toBeInTheDocument();
    });
  });

  it('does not call commentsAPI.add when the textarea is empty', async () => {
    render(<CommentSection taskId="task-1" isOpen={true} onClose={jest.fn()} />);

    await waitFor(() => screen.getByText('Post Comment'));

    fireEvent.click(screen.getByText('Post Comment'));
    expect(commentsAPI.add).not.toHaveBeenCalled();
  });

  it('shows comment count in the header', async () => {
    render(<CommentSection taskId="task-1" isOpen={true} onClose={jest.fn()} />);

    await waitFor(() => {
      expect(screen.getByText('Comments (2)')).toBeInTheDocument();
    });
  });
});
