'use client';

import { useEffect, useState } from 'react';
import { aiAPI } from '@/services/api';

// Shared across every AI affordance (task-description generation, project
// summaries) so each one checks once, not independently on every mount —
// and so they all agree on whether to render at all rather than each
// showing a button that 503s the moment OPENAI_API_KEY isn't configured.
let cached = null;

export function useAIStatus() {
  const [enabled, setEnabled] = useState(cached ?? false);

  useEffect(() => {
    if (cached !== null) return;
    aiAPI.getStatus()
      .then((res) => {
        cached = res.data.enabled;
        setEnabled(cached);
      })
      .catch(() => {
        cached = false;
        setEnabled(false);
      });
  }, []);

  return enabled;
}
