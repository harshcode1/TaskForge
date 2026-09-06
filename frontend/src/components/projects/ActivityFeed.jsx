'use client';

import { useEffect, useState, useCallback } from 'react';
import { activityAPI } from '@/services/api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { History, Plus, Pencil, Trash2 } from 'lucide-react';

// No date library in this project for something this small — a plain
// formatter is fine and one less dependency to ship.
function timeAgo(isoString) {
  const seconds = Math.floor((Date.now() - new Date(isoString).getTime()) / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}d ago`;
  return new Date(isoString).toLocaleDateString();
}

const EVENT_META = {
  CREATED: { verb: 'created', Icon: Plus, color: 'text-[#5fd39a]' },
  UPDATED: { verb: 'updated', Icon: Pencil, color: 'text-[#6ea8fe]' },
  DELETED: { verb: 'deleted', Icon: Trash2, color: 'text-red-400' },
};

/**
 * The durable counterpart to the live "Live" toast feed — that channel is
 * WebSocket-only and gone the moment you miss it, this is what a user
 * opening the project days later actually sees. Exposed via GET
 * /api/activity/{projectId}, written from the same TaskController#broadcast
 * call site that pushes the live event, so the two can't drift apart.
 *
 * `refreshToken` is an optional prop the parent can bump (e.g. after a live
 * WebSocket event) to refetch without the user needing to switch tabs away
 * and back.
 */
export default function ActivityFeed({ projectId, refreshToken }) {
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchActivity = useCallback(async () => {
    try {
      const response = await activityAPI.getForProject(projectId);
      setEntries(response.data);
    } catch (error) {
      console.error('Error fetching activity:', error);
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    if (!projectId) return;
    fetchActivity();
  }, [projectId, refreshToken, fetchActivity]);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Activity</CardTitle>
        <CardDescription>
          Recent task changes on this project — the last 50 events.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="flex items-center justify-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
          </div>
        ) : entries.length === 0 ? (
          <div className="text-center py-8">
            <History className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
            <p className="text-muted-foreground">No activity yet</p>
          </div>
        ) : (
          <ul className="space-y-3">
            {entries.map((entry) => {
              const meta = EVENT_META[entry.type] || EVENT_META.UPDATED;
              const Icon = meta.Icon;
              return (
                <li key={entry.id} className="flex items-start gap-3 text-sm">
                  <Icon className={`h-4 w-4 mt-0.5 shrink-0 ${meta.color}`} />
                  <div className="flex-1 min-w-0">
                    <span className="font-medium">{entry.actorName}</span>{' '}
                    <span className="text-muted-foreground">{meta.verb}</span>{' '}
                    <span className="font-medium">&ldquo;{entry.taskTitle}&rdquo;</span>
                  </div>
                  <span className="text-xs text-muted-foreground whitespace-nowrap">
                    {timeAgo(entry.createdAt)}
                  </span>
                </li>
              );
            })}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}
