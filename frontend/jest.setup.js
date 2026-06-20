// Extend Jest's expect with Testing Library matchers
// e.g. expect(element).toBeInTheDocument()
require('@testing-library/jest-dom');

// Mock next/navigation (useRouter, useParams, etc.)
jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: jest.fn(),
    replace: jest.fn(),
    back: jest.fn(),
    prefetch: jest.fn(),
  }),
  useParams: () => ({}),
  usePathname: () => '/',
}));

// Mock next/link to render a plain <a> in tests
jest.mock('next/link', () => {
  const React = require('react');
  return function MockLink({ href, children, ...props }) {
    return React.createElement('a', { href, ...props }, children);
  };
});

// Mock react-hot-toast so tests don't need a Toaster mounted
jest.mock('react-hot-toast', () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
    loading: jest.fn(),
  },
  default: {
    success: jest.fn(),
    error: jest.fn(),
  },
}));

// Mock next-themes
jest.mock('next-themes', () => ({
  useTheme: () => ({ theme: 'light', setTheme: jest.fn() }),
  ThemeProvider: ({ children }) => children,
}));

// Silence console.error in tests for known React warnings
const originalError = console.error;
beforeAll(() => {
  console.error = (...args) => {
    if (
      typeof args[0] === 'string' &&
      (args[0].includes('Warning:') || args[0].includes('act('))
    ) return;
    originalError.call(console, ...args);
  };
});
afterAll(() => {
  console.error = originalError;
});
