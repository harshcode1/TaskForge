import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { FileQuestion } from 'lucide-react';

export default function NotFound() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-background px-4 text-center">
      <div className="h-14 w-14 mb-6 flex items-center justify-center rounded-lg bg-primary/10 text-primary">
        <FileQuestion className="h-7 w-7" />
      </div>
      <p className="font-mono text-sm font-semibold uppercase tracking-widest text-primary mb-2">404</p>
      <h1 className="text-2xl font-bold mb-2">This page doesn&apos;t exist.</h1>
      <p className="text-muted-foreground mb-8 max-w-sm">
        The link might be broken, or the page may have moved.
      </p>
      <Link href="/">
        <Button>Back to home</Button>
      </Link>
    </div>
  );
}
