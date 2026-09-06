'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { motion } from 'framer-motion';
import { useTheme } from 'next-themes';
import ProtectedRoute from '@/components/auth/ProtectedRoute';
import Navbar from '@/components/layout/Navbar';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { useAuth } from '@/contexts/AuthContext';
import { Sun, Moon, Bell, LogOut } from 'lucide-react';

// No backend endpoint exists for notification preferences (there's no user
// preferences table yet) — this is deliberately stored locally and labeled
// as such, rather than presenting a toggle that quietly does nothing or,
// worse, implies it syncs across devices when it doesn't.
const PREF_KEY = 'taskforge_email_reminders_pref';

export default function SettingsPage() {
  const { theme, setTheme } = useTheme();
  const { logout, user } = useAuth();
  const router = useRouter();
  const [emailReminders, setEmailReminders] = useState(true);

  const handleLogout = () => {
    logout();
    // Same as Navbar's handleLogout — see its comment for why this is '/'
    // and not '/login'.
    router.push('/');
  };

  useEffect(() => {
    const stored = localStorage.getItem(PREF_KEY);
    if (stored !== null) setEmailReminders(stored === 'true');
  }, []);

  const toggleReminders = () => {
    const next = !emailReminders;
    setEmailReminders(next);
    localStorage.setItem(PREF_KEY, String(next));
  };

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-background">
        <Navbar />
        <div className="container mx-auto max-w-2xl px-4 py-8">
          <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}>
            <h1 className="text-3xl font-bold mb-1">Settings</h1>
            <p className="text-muted-foreground mb-8">Preferences for your account on this device.</p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.1 }}
            className="space-y-6"
          >
            <Card>
              <CardHeader>
                <CardTitle>Appearance</CardTitle>
                <CardDescription>Switch between light and dark. Applies immediately.</CardDescription>
              </CardHeader>
              <CardContent className="flex gap-3">
                <Button
                  type="button"
                  variant={theme === 'light' ? 'default' : 'outline'}
                  onClick={() => setTheme('light')}
                  className="flex-1"
                >
                  <Sun className="h-4 w-4" />
                  Light
                </Button>
                <Button
                  type="button"
                  variant={theme === 'dark' ? 'default' : 'outline'}
                  onClick={() => setTheme('dark')}
                  className="flex-1"
                >
                  <Moon className="h-4 w-4" />
                  Dark
                </Button>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Notifications</CardTitle>
                <CardDescription>Stored on this device only — not synced across browsers yet.</CardDescription>
              </CardHeader>
              <CardContent>
                <button
                  type="button"
                  onClick={toggleReminders}
                  className="flex w-full items-center justify-between rounded-lg border border-border p-3 text-left transition-colors hover:border-primary/40"
                >
                  <span className="flex items-center gap-2 text-sm font-medium">
                    <Bell className="h-4 w-4 text-muted-foreground" />
                    Daily due-task email reminders
                  </span>
                  <span
                    className={`relative h-6 w-11 shrink-0 rounded-full transition-colors ${emailReminders ? 'bg-primary' : 'bg-muted'}`}
                  >
                    <span
                      className={`absolute top-0.5 h-5 w-5 rounded-full bg-background shadow transition-transform ${emailReminders ? 'translate-x-5' : 'translate-x-0.5'}`}
                    />
                  </span>
                </button>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Account</CardTitle>
                <CardDescription>Signed in as {user?.email}</CardDescription>
              </CardHeader>
              <CardContent>
                <Separator className="mb-4" />
                <Button type="button" variant="outline" onClick={handleLogout} className="text-destructive hover:text-destructive">
                  <LogOut className="h-4 w-4" />
                  Log out
                </Button>
              </CardContent>
            </Card>
          </motion.div>
        </div>
      </div>
    </ProtectedRoute>
  );
}
