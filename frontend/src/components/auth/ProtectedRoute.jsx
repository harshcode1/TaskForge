'use client';

import { useAuth, isLoggingOut } from '@/contexts/AuthContext';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

export default function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading || isAuthenticated) return;
    // Small delay to prevent race condition with login state updates
    const timer = setTimeout(() => {
      // isLoggingOut means this isAuthenticated=false transition was caused
      // by an explicit logout, which already owns its own navigation (e.g.
      // Navbar's handleLogout pushes to '/') — stand down instead of
      // overriding it with '/login'. Without this, two timing-based fixes
      // (cancel on unmount, check window.location.pathname at fire-time)
      // both still lost the race: Next keeps the old route mounted during
      // a client-side transition, and dev-mode transitions routinely take
      // longer than this timer's own delay. See AuthContext.js.
      if (isLoggingOut) return;
      router.push('/login');
    }, 100);
    return () => clearTimeout(timer);
  }, [isAuthenticated, loading, router]);

  // Both branches below render the same spinner rather than null. Returning
  // null here used to cause a blank black frame on every sign-out: logout()
  // flips isAuthenticated synchronously, this component re-renders and
  // returns null *before* router.push('/login') actually completes the
  // navigation, so the user saw an empty page for a beat. A spinner is
  // correct either way — we're either still checking auth, or we know we're
  // unauthenticated and are already navigating away.
  if (loading || !isAuthenticated) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-primary"></div>
      </div>
    );
  }

  return children;
}

