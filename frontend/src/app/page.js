'use client';

import { useAuth } from '@/contexts/AuthContext';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import Link from 'next/link';
import { toast } from 'react-hot-toast';
import { CheckCircle, Users, Calendar, BarChart3, Github, ShieldCheck, UserCog, User as UserIcon, Loader2, Sparkles } from 'lucide-react';

const REPO_URL = 'https://github.com/harshcode1/TaskForge';

// Fixed accounts seeded by DemoDataSeeder on the backend — same three
// accounts, same populated project data, every time the app boots. See
// backend/.../config/DemoDataSeeder.java for what's actually behind each one.
const DEMO_ACCOUNTS = [
  {
    role: 'ADMIN',
    label: 'Admin',
    email: 'demo-admin@taskforge.dev',
    description: 'Owns every project — full visibility and control.',
    icon: ShieldCheck,
  },
  {
    role: 'MANAGER',
    label: 'Manager',
    email: 'demo-manager@taskforge.dev',
    description: 'Manages two projects, assigned across both.',
    icon: UserCog,
  },
  {
    role: 'MEMBER',
    label: 'Member',
    email: 'demo-member@taskforge.dev',
    description: "A contributor's view — assigned tasks, one project.",
    icon: UserIcon,
  },
];
const DEMO_PASSWORD = 'Demo1234';

const fadeUp = {
  hidden: { opacity: 0, y: 16 },
  visible: (i = 0) => ({
    opacity: 1,
    y: 0,
    transition: { duration: 0.5, delay: i * 0.08, ease: [0.25, 0.46, 0.45, 0.94] },
  }),
};

export default function HomePage() {
  const { isAuthenticated, loading, login } = useAuth();
  const router = useRouter();
  const [demoLoading, setDemoLoading] = useState(null);

  useEffect(() => {
    if (!loading && isAuthenticated) {
      router.push('/dashboard');
    }
  }, [isAuthenticated, loading, router]);

  const tryDemo = async (account) => {
    setDemoLoading(account.role);
    const result = await login(account.email, DEMO_PASSWORD);
    if (result.success) {
      toast.success(`Signed in as ${account.label}`);
      router.push('/dashboard');
    } else {
      toast.error(result.error || 'Demo login failed');
      setDemoLoading(null);
    }
  };

  // Same spinner for both cases — see the comment in ProtectedRoute.jsx for
  // why returning null here (as this used to) causes a blank black flash:
  // isAuthenticated flips true right after a successful demo login, this
  // component re-renders and used to return null before router.push
  // finished navigating to /dashboard.
  if (loading || isAuthenticated) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Navigation */}
      <nav className="border-b border-border">
        <div className="container mx-auto px-4">
          <div className="flex h-16 items-center justify-between">
            <div className="flex items-center space-x-2">
              <div className="h-8 w-8 bg-primary rounded-md flex items-center justify-center">
                <span className="text-primary-foreground font-mono font-bold text-sm">TF</span>
              </div>
              <span className="font-mono font-bold text-xl">TaskForge</span>
            </div>
            <div className="flex items-center gap-2 sm:gap-4">
              <a
                href={REPO_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="hidden sm:inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
              >
                <Github className="h-4 w-4" />
                Source
              </a>
              {/* Visible from the very top of the page, no scrolling required
                  — jumps down to the actual demo picker in the hero. */}
              <a href="#live-demo">
                <Button variant="outline" size="sm" className="border-primary/40 text-primary hover:bg-primary/10 hover:text-primary">
                  <Sparkles className="h-4 w-4" />
                  Live Demo
                </Button>
              </a>
              <Link href="/login">
                <Button variant="ghost">Login</Button>
              </Link>
              <Link href="/register">
                <Button>Get Started</Button>
              </Link>
            </div>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="relative overflow-hidden py-24 px-4">
        <div className="blueprint-grid pointer-events-none absolute inset-0 [mask-image:radial-gradient(ellipse_60%_60%_at_50%_0%,black,transparent)]" />
        <div className="container relative mx-auto text-center">
          <motion.p
            initial="hidden"
            animate="visible"
            custom={0}
            variants={fadeUp}
            className="font-mono text-xs font-semibold uppercase tracking-widest text-primary mb-4"
          >
            Open source · Self-hosted
          </motion.p>
          <motion.h1
            initial="hidden"
            animate="visible"
            custom={1}
            variants={fadeUp}
            className="font-mono text-4xl md:text-6xl font-bold mb-6 tracking-tight"
          >
            A Jira-style tracker
            <span className="text-primary block">you actually control.</span>
          </motion.h1>
          <motion.p
            initial="hidden"
            animate="visible"
            custom={2}
            variants={fadeUp}
            className="text-xl text-muted-foreground mb-10 max-w-2xl mx-auto"
          >
            Kanban boards, role-based projects, and task tracking — a real Spring Boot API
            behind a Next.js frontend, backed by a real test suite. Run it with one Docker command.
          </motion.p>

          {/* Live demo — no signup, pick a role, see a fully populated instance.
              Its own bordered, tinted card (not just more hero text) so it
              reads as a distinct widget at a glance, plus an #anchor the nav
              button above jumps straight to. */}
          <motion.div
            id="live-demo"
            initial="hidden"
            animate="visible"
            custom={3}
            variants={fadeUp}
            className="mx-auto max-w-3xl scroll-mt-24 rounded-xl border border-primary/30 bg-primary/5 p-6 sm:p-8"
          >
            <p className="font-mono text-sm font-semibold uppercase tracking-widest text-primary mb-1">
              ▸ Try it live — no signup
            </p>
            <p className="text-sm text-muted-foreground mb-5">
              Pick a role, see a fully populated instance instantly.
            </p>
            <div className="grid gap-4 sm:grid-cols-3">
              {DEMO_ACCOUNTS.map((account, i) => {
                const Icon = account.icon;
                const isLoading = demoLoading === account.role;
                return (
                  <motion.button
                    key={account.role}
                    type="button"
                    onClick={() => tryDemo(account)}
                    disabled={demoLoading !== null}
                    whileHover={{ y: -3 }}
                    whileTap={{ scale: 0.98 }}
                    initial="hidden"
                    animate="visible"
                    custom={4 + i}
                    variants={fadeUp}
                    className="group flex flex-col items-center gap-2 rounded-lg border border-border bg-card p-5 text-left transition-colors hover:border-primary/50 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    <span className="flex h-10 w-10 items-center justify-center rounded-md bg-primary/10 text-primary">
                      {isLoading ? <Loader2 className="h-5 w-5 animate-spin" /> : <Icon className="h-5 w-5" />}
                    </span>
                    <span className="font-mono font-semibold">{account.label}</span>
                    <span className="text-center text-xs text-muted-foreground">{account.description}</span>
                  </motion.button>
                );
              })}
            </div>
          </motion.div>

          <motion.div
            initial="hidden"
            animate="visible"
            custom={7}
            variants={fadeUp}
            className="mt-10 flex flex-col sm:flex-row gap-4 justify-center"
          >
            <Link href="/register">
              <Button size="lg" variant="outline" className="w-full sm:w-auto">
                Create your own account
              </Button>
            </Link>
            <a href={REPO_URL} target="_blank" rel="noopener noreferrer">
              <Button variant="ghost" size="lg" className="w-full sm:w-auto">
                <Github className="h-4 w-4" />
                View source
              </Button>
            </a>
          </motion.div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 px-4 bg-muted/50">
        <div className="container mx-auto">
          <h2 className="font-mono text-3xl font-bold text-center mb-12">
            Everything you need to manage projects
          </h2>
          <motion.div
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true, margin: '-80px' }}
            className="grid md:grid-cols-2 lg:grid-cols-4 gap-6"
          >
            {[
              { icon: CheckCircle, title: 'Task Management', body: 'Create, assign, and track tasks with a drag-and-drop Kanban board across four status lanes.' },
              { icon: Users, title: 'Team Collaboration', body: 'Invite members, assign project roles, and discuss work in threaded task comments.' },
              { icon: Calendar, title: 'Project Planning', body: 'Due dates, priorities, and automatic overdue detection, with daily email reminders.' },
              { icon: BarChart3, title: 'Analytics & Reports', body: 'Completion rate, status/priority breakdowns, and per-assignee workload, per project.' },
            ].map((f, i) => (
              <motion.div key={f.title} custom={i} variants={fadeUp}>
                <Card className="h-full transition-colors hover:border-primary/40">
                  <CardHeader>
                    <f.icon className="h-10 w-10 text-primary mb-2" />
                    <CardTitle>{f.title}</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <CardDescription>{f.body}</CardDescription>
                  </CardContent>
                </Card>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-4">
        <div className="container mx-auto text-center">
          <h2 className="font-mono text-3xl font-bold mb-6">
            Self-host it, or just try it out.
          </h2>
          <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
            Spin up the whole stack — MySQL, the Spring Boot API, and this frontend — with a single
            <code className="mx-1.5 rounded bg-muted px-1.5 py-0.5 font-mono text-base">docker compose up</code>
            or use the live demo above to try it right now.
          </p>
          <Link href="/register">
            <Button size="lg">
              Get Started
            </Button>
          </Link>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-border py-8 px-4">
        <div className="container mx-auto flex flex-col sm:flex-row items-center justify-between gap-4 text-sm text-muted-foreground">
          <p>&copy; {new Date().getFullYear()} TaskForge. Built by <a href="https://github.com/harshcode1" target="_blank" rel="noopener noreferrer" className="underline underline-offset-2 hover:text-foreground">Harsh Soni</a>.</p>
          <a
            href={REPO_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1.5 hover:text-foreground transition-colors"
          >
            <Github className="h-4 w-4" />
            github.com/harshcode1/TaskForge
          </a>
        </div>
      </footer>
    </div>
  );
}
