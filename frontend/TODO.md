# TaskForge Frontend — Critical Bug Fix TODO

> **Author**: Senior Architect Review — 2026-06-20  
> **Scope**: 5 critical bugs + all broken chains they expose  
> **Rule**: Every change must be validated end-to-end: backend DTO → api.js → component state → UI render

---

## STATUS LEGEND
- [ ] Not started  
- [~] In progress  
- [x] Done  
- [!] Blocked / needs attention

---

## BUG #1 — Update Project: No UI Exists

**Root cause**: Backend has `PUT /api/projects/{id}` (owner-only) but frontend has no `projectsAPI.update()` and no edit UI.

**Impact**: Users can never rename or redescribe a project after creation.

**Chain**:
```
Edit button click
  → projectsAPI.update(id, name, description)
    → PUT /api/projects/{id} {name, description}
      → ProjectService.updateProject() [owner check]
        → returns ProjectResponseDTO {id, name, description, createdAt, owner}
  → update projects[] local state with response.data
  → close dialog
  → toast.success
```

### Tasks
- [x] **1.1** Add `update(id, name, description)` to `projectsAPI` in `src/services/api.js`
- [x] **1.2** Add edit state: `editDialogOpen`, `editingProject`, `updating` to `src/app/projects/page.js`
- [x] **1.3** Add Edit (pencil) icon button next to Delete on each project card
- [x] **1.4** Add Edit Project Dialog in `src/app/projects/page.js` with name + description fields pre-filled
- [x] **1.5** Add `handleEditProject()` handler: validate name, call `projectsAPI.update()`, update local state
- [x] **1.6** Add Edit Project button + dialog in `src/app/projects/[id]/page.js` header (owner edits from detail view too)
- [x] **1.7** Write test: `projectsAPI.update()` sends correct PUT payload

### Validation Checklist
- [ ] Edit button appears on project cards
- [ ] Dialog pre-fills current name + description
- [ ] Empty name shows validation error, does NOT call API
- [ ] Successful update reflects immediately in the card (no page reload)
- [ ] Non-owner gets 403 from backend → toast.error shown
- [ ] Cancel button closes dialog without changes

---

## BUG #2 — Delete Comment: No UI + Comment Fields Broken

**Root cause (2a)**: Backend has `DELETE /api/comments/{id}` (author-only) but frontend has no `commentsAPI.delete()` and no delete button in `CommentSection.jsx`.

**Root cause (2b — BLOCKING)**: `CommentSection.jsx` reads wrong field names from the Comment entity:
- `comment.text` → backend sends `comment.content` → always shows BLANK text
- `comment.authorName` → backend sends `comment.author.name` → always shows "U" initials
- `comment.authorEmail` → backend sends `comment.author.email` → always undefined

This means **all comments have been broken since day 1** — users see blank comments with "U" avatars.

**Backend Comment JSON shape** (raw entity, not a DTO):
```json
{
  "id": "uuid",
  "content": "the text...",
  "createdAt": "2024-01-01T10:00:00",
  "author": {
    "id": "uuid",
    "name": "John Doe",
    "email": "john@example.com",
    "role": "MEMBER",
    "createdAt": "..."
  }
}
```

**Chain (delete)**:
```
Trash icon click (visible only to comment.author.email === user.email)
  → confirm (styled dialog or window.confirm)
    → commentsAPI.delete(commentId)
      → DELETE /api/comments/{commentId}
        → CommentService.deleteComment() [author check]
          → 204 No Content
  → remove comment from comments[] local state by id
  → toast.success
```

### Tasks
- [x] **2.1** Fix `comment.text` → `comment.content` in `CommentSection.jsx`
- [x] **2.2** Fix `comment.authorName` → `comment.author?.name` in `CommentSection.jsx`
- [x] **2.3** Fix `comment.authorEmail` → `comment.author?.email` in `CommentSection.jsx`
- [x] **2.4** Fix avatar initials: use `comment.author?.name` not `comment.authorEmail`
- [x] **2.5** Add `delete(id)` to `commentsAPI` in `src/services/api.js`
- [x] **2.6** Add `handleDeleteComment(id)` in `CommentSection.jsx`
- [x] **2.7** Show delete button only when `comment.author?.email === user?.email`
- [x] **2.8** Remove comment from local `comments[]` on success
- [x] **2.9** Write test: comment fields render correctly from backend shape
- [x] **2.10** Write test: delete button visible only to author

### Validation Checklist
- [ ] Existing comments now show actual text (not blank)
- [ ] Author names show correctly (not "U")
- [ ] Delete button visible only to the author of that comment
- [ ] Deleting removes from UI immediately (optimistic)
- [ ] Non-author gets 403 from backend (safeguard)

---

## BUG #3 — PENDING Status: Invisible in Kanban + Missing from UI

**Root cause**: Backend `TaskStatus` enum has 4 values: `TODO | IN_PROGRESS | DONE | PENDING`. Frontend Kanban only has 3 columns (TODO, IN_PROGRESS, DONE) and TaskModal only offers 3 status options. Tasks with `PENDING` status from the backend drop into the void — they exist in the DB but never appear on the board.

**Impact**: Any task set to PENDING (e.g. via API or reminder service) vanishes from the UI.

**All affected locations**:
- `KanbanBoard.jsx` — missing PENDING column
- `TaskModal.jsx` — missing PENDING in status select
- `TaskDetailModal.jsx` — `getStatusColor` has no PENDING case
- `TaskFilters.jsx` — missing PENDING in status filter + quick stats
- `projects/[id]/page.js` — `getTaskStats()` doesn't count PENDING + stat cards are wrong
- `dashboard/page.js` — `getStatusColor` + `getStatusIcon` have no PENDING case

### Tasks
- [x] **3.1** Add PENDING column to `KanbanBoard.jsx` `columns` array: `{ id: 'PENDING', title: 'Pending', status: 'PENDING' }`
- [x] **3.2** Add PENDING color in `KanbanBoard.jsx` `getColumnColor()`: orange/amber theme
- [x] **3.3** Update `KanbanBoard.jsx` grid from `md:grid-cols-3` to `grid-cols-1 sm:grid-cols-2 xl:grid-cols-4`
- [x] **3.4** Add `<SelectItem value="PENDING">Pending</SelectItem>` to `TaskModal.jsx` status select
- [x] **3.5** Add PENDING case to `TaskDetailModal.jsx` `getStatusColor()`: amber/orange colors
- [x] **3.6** Add PENDING to `TaskFilters.jsx` status filter select
- [x] **3.7** Update `TaskFilters.jsx` quick stats to show `taskCounts.pending` (5th stat)
- [x] **3.8** Update `TaskFilters.jsx` prop default: `taskCounts = { total: 0, todo: 0, inProgress: 0, done: 0, pending: 0 }`
- [x] **3.9** Update `getTaskStats()` in `projects/[id]/page.js` to compute `pending` count
- [x] **3.10** Add PENDING stat card in `projects/[id]/page.js` (5th card, update grid)
- [x] **3.11** Add PENDING case to `dashboard/page.js` `getStatusColor()`: orange
- [x] **3.12** Add PENDING case to `dashboard/page.js` `getStatusIcon()`: Clock icon (paused)
- [x] **3.13** Write test: KanbanBoard renders 4 columns when tasks include PENDING status
- [x] **3.14** Write test: TaskModal status select includes PENDING option

### Validation Checklist
- [ ] A task with `status: PENDING` appears in the Pending column on the board
- [ ] Dragging a task to the Pending column updates its status (PENDING in API call)
- [ ] TaskModal status dropdown shows Pending option
- [ ] Creating a task with Pending status works end-to-end
- [ ] Pending count shows in stats cards on project detail
- [ ] Pending badge shows correct amber color in TaskDetailModal

---

## BUG #4 — N+1 API Calls on Dashboard

**Root cause**: `dashboard/page.js` fetches all projects, then loops through each project calling `tasksAPI.getByProject()` — this is 1 + N API calls where N = number of projects. Backend already has `GET /api/dashboard/my-dashboard` which returns all personal stats in 1 call.

**Current flow (broken)**:
```
fetchDashboardData()
  → projectsAPI.getAll()          [1 call]
  → for each project:
      tasksAPI.getByProject(id)   [N calls]  ← N+1 problem
  → compute stats locally
```

**Fixed flow**:
```
fetchDashboardData()
  → projectsAPI.getAll()          [1 call] → recent projects display
  → dashboardAPI.getUserDashboard() [1 call] → stats from backend
  → tasksAPI.getMyTasks()         [1 call] → recent tasks (my assigned tasks)
Total: 3 calls (was: N+1)
```

**`DashboardResponse` shape** (what `my-dashboard` returns):
```json
{
  "taskStatusCounts": {"TODO": 3, "IN_PROGRESS": 2, "DONE": 5, "PENDING": 1},
  "tasksPerUser": {"user@email.com": 4},
  "totalTasks": 11,
  "completedTasks": 5,
  "pendingTasks": 1,
  "inProgressTasks": 2,
  "completionRate": 45,
  "overdueTasks": 2,
  "totalMembers": 3
}
```

**Note on stat semantics**: After the fix, stats represent "tasks assigned to me" not "all project tasks". This is more useful for a personal dashboard (e.g., GitHub-style "your work") and semantically labeled accordingly.

### Tasks
- [x] **4.1** Add `getUserDashboard()` to `dashboardAPI` in `src/services/api.js`: `api.get('/dashboard/my-dashboard')`
- [x] **4.2** Import `dashboardAPI` and `tasksAPI` in `dashboard/page.js`
- [x] **4.3** Replace N+1 loop with `Promise.all([dashboardAPI.getUserDashboard(), tasksAPI.getMyTasks()])` — run in parallel
- [x] **4.4** Map `DashboardResponse` fields to `stats` state: `totalTasks`, `completedTasks`, `pendingTasks`(non-DONE), `inProgressTasks`
- [x] **4.5** Use `tasksAPI.getMyTasks()` response for the "Recent Tasks" list (5 most recent)
- [x] **4.6** Update stat card labels: "Total Tasks" → "My Tasks" (they're now personal stats)
- [x] **4.7** Handle the case where `getUserDashboard()` fails gracefully (backend email reminder edge case)
- [x] **4.8** Update `stats` state shape to include `inProgress` (currently only totalProjects, totalTasks, completedTasks, pendingTasks)
- [x] **4.9** Write test: `fetchDashboardData()` calls the 3 correct endpoints (no loop)

### Validation Checklist
- [ ] Dashboard loads with exactly 3 API calls (verify in browser network tab)
- [ ] Stats show correct numbers from `my-dashboard` endpoint
- [ ] Recent tasks show tasks assigned to me (from `getMyTasks()`)
- [ ] Recent projects still show (from `getAll()`)
- [ ] If user has 20 projects, dashboard still makes exactly 3 calls (not 21)
- [ ] Handles empty state: 0 projects, 0 tasks

---

## BUG #5 — My Tasks Page: Defined but Never Built

**Root cause**: `tasksAPI.getMyTasks()` (calls `GET /api/tasks`) exists in `api.js` but there is no page that uses it. The "My Tasks" concept is missing from the navigation entirely.

**What `GET /api/tasks` returns**: `List<TaskDTO>` — all tasks where `assigneeEmail` == current user's email, across all projects.

**`TaskDTO` shape**:
```json
{
  "id": "uuid",
  "title": "...",
  "description": "...",
  "priority": "HIGH",
  "status": "TODO",
  "createdAt": "...",
  "projectId": "uuid",
  "assigneeEmail": "...",
  "assigneeName": "...",
  "dueDate": "2025-12-31"
}
```

**Note**: `projectId` is included — allows "View in Project" link per task.

### Tasks
- [x] **5.1** Create `src/app/my-tasks/page.js` — protected route
- [x] **5.2** Fetch with `tasksAPI.getMyTasks()` on mount
- [x] **5.3** Group tasks by status: TODO | IN_PROGRESS | PENDING | DONE sections
- [x] **5.4** Add status filter tabs (All / To Do / In Progress / Pending / Done) at top
- [x] **5.5** Add priority badge, due date, overdue indicator per task row
- [x] **5.6** Add "View in Project" button per task → link to `/projects/{task.projectId}`
- [x] **5.7** Add empty state (no tasks assigned to me)
- [x] **5.8** Fix `Navbar.jsx` broken Projects link: `href={isAuthenticated ? '/projects/${user?.projectId}' : '/projects'}` → **always `href="/projects"`** (`user?.projectId` does not exist)
- [x] **5.9** Add "My Tasks" link to `Navbar.jsx` nav links
- [x] **5.10** Write test: My Tasks page renders tasks grouped correctly

### Validation Checklist
- [ ] `/my-tasks` route renders without 404
- [ ] Only shows tasks where I am the assignee
- [ ] Status filter tabs work correctly
- [ ] "View in Project" link navigates to correct project
- [ ] Overdue tasks highlighted in red
- [ ] Navbar "My Tasks" link works from all pages

---

## BONUS BUG — Navbar Projects Link (Discovered During Fix #5)

**Root cause**: 
```jsx
<Link href={isAuthenticated ? `/projects/${user?.projectId}` : "/projects"}>
```
`user?.projectId` does not exist on the User object (auth response only has `{ userId, name, email, role }`). This causes the Projects link to always go to `/projects/undefined` when logged in. This is **silently broken** in production.

- [x] **B.1** Fix to always use `href="/projects"` (covered in 5.8 above)

---

## TEST SETUP — Frontend Has Zero Tests

**Current state**: No `jest.config.js`, no `jest.setup.js`, no test files anywhere.

**Target**: Minimal Jest + React Testing Library setup with tests for all modified code.

### Tasks
- [x] **T.1** Add jest dependencies to `package.json`: `jest`, `jest-environment-jsdom`, `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`, `babel-jest`, `@babel/preset-env`, `@babel/preset-react`
- [x] **T.2** Create `jest.config.js`
- [x] **T.3** Create `jest.setup.js`
- [x] **T.4** Create `src/__tests__/api.test.js` — test all API functions
- [x] **T.5** Create `src/__tests__/CommentSection.test.jsx` — test field mapping + delete
- [x] **T.6** Create `src/__tests__/KanbanBoard.test.jsx` — test 4 columns + PENDING
- [x] **T.7** Create `src/__tests__/TaskModal.test.jsx` — test PENDING in status select
- [x] **T.8** Add `"test": "jest"` and `"test:watch": "jest --watch"` to `package.json` scripts

---

## IMPLEMENTATION ORDER (Dependency Graph)

```
1. api.js                     ← everything depends on this, fix first
2. CommentSection.jsx         ← field fix is blocking, unblocks all comment tests
3. KanbanBoard.jsx            ← PENDING column (independent)
4. TaskModal.jsx              ← PENDING status option (independent)
5. TaskDetailModal.jsx        ← PENDING color (independent)
6. TaskFilters.jsx            ← PENDING stats + filter (independent)
7. projects/[id]/page.js      ← depends on updated getTaskStats + PENDING stats
8. projects/page.js           ← depends on projectsAPI.update
9. dashboard/page.js          ← depends on dashboardAPI.getUserDashboard
10. my-tasks/page.js          ← new page, depends on api.js
11. Navbar.jsx                ← add My Tasks link, fix broken Projects link
12. jest.config.js + setup    ← test infrastructure
13. Test files                ← write all tests last
```

---

## RISK TABLE

| Change | Risk | Mitigation |
|---|---|---|
| `comment.text` → `comment.content` | Medium — comments were broken anyway, this is a fix | Verify against actual backend response |
| Adding PENDING column | Low — additive change | PENDING tasks didn't render before; now they will |
| Dashboard N+1 fix | Medium — semantics change (all tasks → my tasks) | Update stat card labels to reflect "my" tasks |
| my-tasks page | Low — new page, no existing code touched | — |
| api.js additions | Low — additive only | Existing functions unchanged |
| Navbar Projects link fix | Low — fixes broken link | `/projects` is correct target |

---

## DEFINITION OF DONE

All 5 bugs are fixed when:
1. `[ ]` All items in each bug section are checked `[x]`
2. `[ ]` `npm run build` passes without errors
3. `[ ]` `npm test` passes (all test files green)
4. `[ ]` Manual smoke test: login → dashboard loads in 3 API calls → create task with PENDING → Kanban shows it → comment on task → comment text visible → delete own comment → disappears → edit project name → name updates → My Tasks page shows assigned tasks
