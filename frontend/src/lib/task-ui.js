// Single source of truth for how task status and priority render across the
// app. Previously this same status->color / priority->color mapping was
// copy-pasted independently in ~6 files (KanbanBoard, TaskDetailModal,
// dashboard, project detail, my-tasks) with drifting values between them —
// classic duplication that goes stale the moment one call site is edited and
// the others aren't. Import from here instead of re-deriving it locally.

export const STATUS_META = {
  TODO: { label: 'To Do', badgeClass: 'status-badge-neutral', dotClass: 'status-dot-neutral' },
  IN_PROGRESS: { label: 'In Progress', badgeClass: 'status-badge-progress', dotClass: 'status-dot-progress' },
  PENDING: { label: 'Pending', badgeClass: 'status-badge-pending', dotClass: 'status-dot-pending' },
  DONE: { label: 'Done', badgeClass: 'status-badge-done', dotClass: 'status-dot-done' },
};

export const PRIORITY_META = {
  LOW: { label: 'Low', badgeClass: 'priority-badge-low' },
  MEDIUM: { label: 'Medium', badgeClass: 'priority-badge-medium' },
  HIGH: { label: 'High', badgeClass: 'priority-badge-high' },
};

export function statusBadgeClass(status) {
  return STATUS_META[status]?.badgeClass ?? STATUS_META.TODO.badgeClass;
}

export function statusDotClass(status) {
  return STATUS_META[status]?.dotClass ?? STATUS_META.TODO.dotClass;
}

export function statusLabel(status) {
  return STATUS_META[status]?.label ?? status;
}

export function priorityBadgeClass(priority) {
  return PRIORITY_META[priority]?.badgeClass ?? PRIORITY_META.MEDIUM.badgeClass;
}

export function priorityLabel(priority) {
  return PRIORITY_META[priority]?.label ?? priority;
}

// Recharts takes literal color strings, not Tailwind classes — these mirror
// the same semantic hues as the CSS above (signal amber / progress blue /
// done green / pending gray / destructive red) so charts and badges agree.
export const CHART_COLORS = {
  amber: '#ffb020',
  blue: '#6ea8fe',
  green: '#5fd39a',
  gray: '#8a8a82',
  red: '#f0665e',
};

export const STATUS_CHART_COLORS = {
  TODO: CHART_COLORS.gray,
  IN_PROGRESS: CHART_COLORS.blue,
  PENDING: CHART_COLORS.amber,
  DONE: CHART_COLORS.green,
};

export const PRIORITY_CHART_COLORS = {
  LOW: CHART_COLORS.gray,
  MEDIUM: CHART_COLORS.amber,
  HIGH: CHART_COLORS.red,
};
