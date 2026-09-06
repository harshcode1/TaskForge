'use client';

import { useAuth } from '@/contexts/AuthContext';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

export default function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !isAuthenticated) {
      // Small delay to prevent race condition with login state updates
      setTimeout(() => {
        if (!isAuthenticated) {
          router.push('/login');
        }
      }, 100);
    }
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

