'use client';

import { useState } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useAIStatus } from '@/hooks/use-ai-status';
import { aiAPI } from '@/services/api';
import { toast } from 'react-hot-toast';
import { Sparkles, Loader2 } from 'lucide-react';

// Only rendered when the backend actually has OPENAI_API_KEY configured
// (useAIStatus) — otherwise this card just doesn't exist, rather than
// showing a "Generate" button that 503s on every click.
export default function AIProjectSummary({ projectId }) {
  const aiEnabled = useAIStatus();
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(false);

  if (!aiEnabled) return null;

  const generate = async () => {
    setLoading(true);
    try {
      const res = await aiAPI.getProjectSummary(projectId);
      setSummary(res.data.text);
    } catch (error) {
      toast.error(error.response?.data?.message || 'AI summary failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card className="border-primary/30 bg-primary/5">
      <CardContent className="pt-6">
        <div className="flex items-start justify-between gap-4">
          <div className="flex items-start gap-3">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
              <Sparkles className="h-4 w-4" />
            </span>
            <div>
              <p className="text-sm font-semibold">AI Status Summary</p>
              {summary ? (
                <p className="mt-1 text-sm text-muted-foreground">{summary}</p>
              ) : (
                <p className="mt-1 text-sm text-muted-foreground">
                  Generate a plain-English summary of where this project stands.
                </p>
              )}
            </div>
          </div>
          <Button variant="outline" size="sm" onClick={generate} disabled={loading} className="shrink-0">
            {loading ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Sparkles className="h-3.5 w-3.5" />}
            {summary ? 'Regenerate' : 'Generate'}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
