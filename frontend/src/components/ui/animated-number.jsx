'use client';

import { useEffect, useRef, useState } from 'react';

/**
 * Counts up from 0 to `value` once, on mount. Small and dependency-free
 * (framer-motion's useSpring/useMotionValue is overkill for a single
 * count-up), used on every stat-card number across the dashboard/board pages.
 */
export default function AnimatedNumber({ value, duration = 700 }) {
  const [display, setDisplay] = useState(0);
  // Dashboard stats start at 0 while loading, then jump to the real number
  // once the API responds — that's a *different* value, not a re-render of
  // the same one, so it needs to animate too. Guarding on "already animated
  // to this exact target" (rather than "ever animated once") handles both
  // the 0->real transition and value===0 genuinely being the final answer.
  const animatedTo = useRef(null);

  useEffect(() => {
    const target = Number(value) || 0;
    if (animatedTo.current === target) return;
    animatedTo.current = target;
    const from = display;
    const startTime = performance.now();
    let frame;
    let completed = false;
    const tick = (now) => {
      const progress = Math.min((now - startTime) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3); // ease-out cubic
      setDisplay(Math.round(from + (target - from) * eased));
      if (progress < 1) {
        frame = requestAnimationFrame(tick);
      } else {
        completed = true;
      }
    };
    frame = requestAnimationFrame(tick);
    return () => {
      cancelAnimationFrame(frame);
      // React 18 StrictMode (dev only) mounts every effect, cleans it up,
      // then mounts it again — which cancels this very first scheduled
      // frame before the browser ever paints it. Without this guard,
      // animatedTo.current was already marked as "reached" from the
      // cancelled attempt, so the real (second) invocation silently
      // skipped animating and the number stayed frozen at 0 forever.
      // Un-marking an incomplete run means the next invocation retries.
      if (!completed) animatedTo.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- `display` is read only to seed `from`; including it would restart the animation every frame
  }, [value, duration]);

  return <>{display}</>;
}
