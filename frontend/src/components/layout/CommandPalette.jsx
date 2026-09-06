'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { useTheme } from 'next-themes';
import {
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command';
import { useAuth } from '@/contexts/AuthContext';
import { projectsAPI, tasksAPI } from '@/services/api';
import { statusLabel } from '@/lib/task-ui';
import {
  LayoutDashboard,
  FolderKanban,
  ClipboardList,
  User,
  Settings,
  Plus,
  SunMoon,
  FolderOpen,
  CheckSquare,
} from 'lucide-react';

/**
 * Global Cmd+K / Ctrl+K command palette — mounted once in the root layout
 * (see app/layout.js) so it's available from every authenticated page
 * without every page needing to render it individually. Renders nothing
 * while logged out; data (projects + my-tasks) is fetched lazily, only on
 * first open, not on every keystroke or page load.
 */
export default function CommandPalette() {
  const { isAuthenticated } = useAuth();
  const router = useRouter();
  const { theme, setTheme } = useTheme();
  const [open, setOpen] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [projects, setProjects] = useState([]);
  const [tasks, setTasks] = useState([]);

  useEffect(() => {
    if (!isAuthenticated) return;
    const handler = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setOpen((o) => !o);
      }
    };
    document.addEventListener('keydown', handler);
    // Also openable by clicking the search affordance in Navbar — a
    // keyboard-only shortcut with no visible entry point is a classic
    // discoverability dead end, so Navbar dispatches this instead of us
    // needing to lift `open` state up into it.
    const openViaClick = () => setOpen(true);
    window.addEventListener('open-command-palette', openViaClick);
    return () => {
      document.removeEventListener('keydown', handler);
      window.removeEventListener('open-command-palette', openViaClick);
    };
  }, [isAuthenticated]);

  // Fetch once, the first time the palette is actually opened — not on
  // mount, so it costs nothing on pages where the user never presses Cmd+K.
  useEffect(() => {
    if (!open || loaded) return;
    Promise.all([projectsAPI.getAll(), tasksAPI.getMyTasks()])
      .then(([p, t]) => {
        setProjects(p.data);
        setTasks(t.data);
        setLoaded(true);
      })
      .catch(() => setLoaded(true)); // fail open — nav/actions still work without search data
  }, [open, loaded]);

  const go = useCallback((path) => {
    router.push(path);
    setOpen(false);
  }, [router]);

  if (!isAuthenticated) return null;

  return (
    <CommandDialog open={open} onOpenChange={setOpen}>
      <CommandInput placeholder="Jump to a project, a task, or run a command..." />
      <CommandList>
        <CommandEmpty>No results.</CommandEmpty>

        {/* Every item gets an explicit `value` — cmdk otherwise derives it
            from the item's text content via a ref walk, which is unreliable
            once children mix an icon element with text (as every item here
            does) and caused a real bug: filtering to one visible match
            still left keyboard selection pointing at a hidden earlier item,
            so Enter navigated to whatever was first before you typed. */}
        <CommandGroup heading="Navigate">
          <CommandItem value="Dashboard" onSelect={() => go('/dashboard')}>
            <LayoutDashboard /> Dashboard
          </CommandItem>
          <CommandItem value="Projects" onSelect={() => go('/projects')}>
            <FolderKanban /> Projects
          </CommandItem>
          <CommandItem value="My Tasks" onSelect={() => go('/my-tasks')}>
            <ClipboardList /> My Tasks
          </CommandItem>
          <CommandItem value="Profile" onSelect={() => go('/profile')}>
            <User /> Profile
          </CommandItem>
          <CommandItem value="Settings" onSelect={() => go('/settings')}>
            <Settings /> Settings
          </CommandItem>
        </CommandGroup>

        <CommandGroup heading="Actions">
          <CommandItem value="New project" onSelect={() => go('/projects')}>
            <Plus /> New project
          </CommandItem>
          <CommandItem value="Toggle theme" onSelect={() => { setTheme(theme === 'dark' ? 'light' : 'dark'); setOpen(false); }}>
            <SunMoon /> Toggle theme
          </CommandItem>
        </CommandGroup>

        {projects.length > 0 && (
          <CommandGroup heading="Projects">
            {projects.map((p) => (
              <CommandItem key={p.id} value={`project ${p.name}`} onSelect={() => go(`/projects/${p.id}`)}>
                <FolderOpen /> {p.name}
              </CommandItem>
            ))}
          </CommandGroup>
        )}

        {tasks.length > 0 && (
          <CommandGroup heading="My Tasks">
            {tasks.slice(0, 8).map((t) => (
              <CommandItem
                key={t.id}
                value={`task ${t.title}`}
                onSelect={() => go(t.projectId ? `/projects/${t.projectId}` : '/my-tasks')}
              >
                <CheckSquare />
                <span className="flex-1 truncate">{t.title}</span>
                <span className="text-xs text-muted-foreground">{statusLabel(t.status)}</span>
              </CommandItem>
            ))}
          </CommandGroup>
        )}
      </CommandList>
    </CommandDialog>
  );
}
