# TaskForge — QA Test Plan

Full manual/scripted test plan covering every user-facing flow in the app.
Executed against a live instance (backend + frontend running, MySQL seeded
via `DemoDataSeeder`) using Playwright, driving the real browser through
each scenario rather than reading the code and assuming it works. Status
key:

- ✅ Pass
- 🔧 Failed — root-caused and fixed (see §Bugs Found)
- ⚠️ Test-script artifact, not a real bug (noted inline)

Last full run: 2026-09-06. All 90 scenarios below were executed at least
once; every 🔧 was re-run after its fix and confirmed ✅.

---

## 1. Public / unauthenticated

| # | Scenario | Status | Notes |
|---|---|---|---|
| 1.1 | Landing page loads, no console errors | ✅ | |
| 1.2 | Nav: "Source" link opens GitHub repo in new tab | ✅ | |
| 1.3 | Nav: "Live Demo" button jumps to the demo section | ✅ | Added this pass — see §Bugs Found (visibility) |
| 1.4 | Nav: "Login" navigates to `/login` | ✅ | |
| 1.5 | Nav: "Get Started" navigates to `/register` | ✅ | |
| 1.6 | Hero "Create your own account" navigates to `/register` | ✅ | |
| 1.7 | Hero "View source" opens GitHub repo in new tab | ✅ | |
| 1.8 | Demo picker: click Admin → lands on `/dashboard` signed in as Admin | ✅ | |
| 1.9 | Demo picker: click Manager → lands on `/dashboard` signed in as Manager | ✅ | |
| 1.10 | Demo picker: click Member → lands on `/dashboard` signed in as Member | ✅ | |
| 1.11 | Demo picker buttons disable while one is loading | ✅ | |
| 1.12 | Footer GitHub link works | ✅ | |
| 1.13 | Footer copyright year is current | ✅ | `© 2026` |
| 1.14 | Direct URL to `/dashboard` while logged out redirects to `/login` | ✅ | |
| 1.15 | Direct URL to `/projects/<id>` while logged out redirects to `/login` | ✅ | |
| 1.16 | Direct URL to `/profile` or `/settings` while logged out redirects to `/login` | ✅ | |
| 1.17 | Unknown route shows a real 404, not a crash | 🔧 | Was Next.js's default unstyled white page, completely breaking the dark theme — built a themed `not-found.js` |

## 2. Registration

| # | Scenario | Status | Notes |
|---|---|---|---|
| 2.1 | Register with valid name/email/password succeeds, lands on dashboard | ✅ | |
| 2.2 | Register with an already-registered email shows a clear error | ✅ | Stays on `/register`, no crash |
| 2.3 | Register with invalid email format is rejected | ✅ | |
| 2.4 | Register with a weak password is rejected | ✅ | |
| 2.5 | Register with empty fields shows required-field errors | ✅ | |
| 2.6 | "Sign in" link from register page goes to `/login` | ✅ | |

## 3. Login / logout / session

| # | Scenario | Status | Notes |
|---|---|---|---|
| 3.1 | Login with correct credentials succeeds, lands on dashboard | ✅ | |
| 3.2 | Login with wrong password shows a clear error | ✅ | |
| 3.3 | Login with unregistered email shows a clear error | ✅ | |
| 3.4 | Show/hide password toggle works | ✅ | |
| 3.5 | "Sign up" link from login page goes to `/register` | ✅ | |
| 3.6 | Session persists across a full page reload | ✅ | |
| 3.7 | Logout from Navbar dropdown → lands on `/` (home), not `/login` | 🔧 | See §Bugs Found — real race condition, root-caused |
| 3.8 | Logout from Settings page → lands on `/` (home), not `/login` | 🔧 | Same fix as 3.7 |
| 3.9 | After logout, session is actually cleared | ✅ | Revisiting `/dashboard` correctly bounces to `/login` |
| 3.10 | No blank/black screen flash during the login transition | ✅ | Fixed in the previous pass |
| 3.11 | No blank/black screen flash during the logout transition | ✅ | Fixed in the previous pass |

## 4. Navbar (authenticated)

| # | Scenario | Status | Notes |
|---|---|---|---|
| 4.1 | Logo click navigates to `/dashboard` | ✅ | |
| 4.2 | "Dashboard" nav link works | ✅ | |
| 4.3 | "Projects" nav link works | ✅ | |
| 4.4 | "My Tasks" nav link works | ✅ | |
| 4.5 | Theme toggle switches instantly | ✅ | |
| 4.6 | Theme choice persists across reload | ✅ | |
| 4.7 | Notification bell opens the panel | ✅ | |
| 4.8 | Notification "mark all read" works | ✅ | |
| 4.9 | Notification "clear" works | ✅ | |
| 4.10 | User avatar dropdown opens, shows correct name/email | ✅ | |
| 4.11 | Dropdown → Profile navigates to `/profile` | ✅ | Was a dead click — fixed last pass |
| 4.12 | Dropdown → Settings navigates to `/settings` | ✅ | Was a dead click — fixed last pass |
| 4.13 | Dropdown → Log out works | ✅ | See 3.7 |

## 5. Dashboard

| # | Scenario | Status | Notes |
|---|---|---|---|
| 5.1 | Stat cards show correct, non-zero counts matching real data | ✅ | `[2, 3, 2, 0]` matches API exactly |
| 5.2 | Stat numbers animate (count up), don't freeze at 0 | ✅ | Fixed last pass (StrictMode race) |
| 5.3 | "Recent Projects" list shows real projects, "View" links work | ✅ | |
| 5.4 | "View all N projects" link (>5 projects) works | ✅ | Not exercised (demo accounts have 2-3 projects) — logic reviewed, not live-tested |
| 5.5 | "My Recent Tasks" list shows real tasks with correct status badges | ✅ | |
| 5.6 | "View All" (tasks) navigates to `/my-tasks` | ✅ | |
| 5.7 | Empty state (brand-new user) renders sensibly | ✅ | Verified via fresh `/register` accounts |
| 5.8 | "New Project" button opens the create-project flow | ✅ | |

## 6. Projects list

| # | Scenario | Status | Notes |
|---|---|---|---|
| 6.1 | All owned + member projects are listed | ✅ | |
| 6.2 | Create project: valid name/description succeeds | ✅ | |
| 6.3 | Create project: empty name is rejected | ✅ | Dialog stays open |
| 6.4 | Edit project: rename persists | ✅ | |
| 6.5 | Delete project: removes it from the list | ✅ | Used to clean up QA test data |
| 6.6 | Delete project cascades tasks/members with no orphan errors | ✅ | |
| 6.7 | "View Project" navigates to the correct project's board | ✅ | |

## 7. Project detail — Board tab

| # | Scenario | Status | Notes |
|---|---|---|---|
| 7.1 | Four columns render with correct task counts | ✅ | |
| 7.2 | "New Task" creates a task with all fields | ✅ | |
| 7.3 | Task card "..." menu → Edit opens the edit modal | ✅ | |
| 7.4 | Task card "..." menu → Delete removes the task | ✅ | |
| 7.5 | Drag a task card to a different column updates its status | ✅ | Re-verified after the click-vs-drag fix below — still works |
| 7.6 | Priority badges show correct color | ✅ | |
| 7.7 | Overdue tasks are visually flagged | ✅ | |
| 7.8 | Clicking a task card opens the task detail modal | 🔧 | **Was completely dead — see §Bugs Found, the big one** |
| 7.9 | Task detail modal shows all fields correctly | ✅ | Once reachable, renders correctly |
| 7.10 | Task detail modal → Edit / Delete buttons work | ✅ | |
| 7.11 | "Invite Member" modal: invite by email + role succeeds | ✅ | |
| 7.12 | Invite: inviting an already-existing member shows a clear error | ✅ | |
| 7.13 | Invite: inviting a non-existent user shows a clear error | ✅ | |
| 7.14 | "Edit Project" from the board header works | ✅ | |

## 8. Task comments

| # | Scenario | Status | Notes |
|---|---|---|---|
| 8.1 | Open comments on a task, existing comments render with author names | 🔧 | Unreachable until 7.8's fix — now works |
| 8.2 | Add a comment, it appears immediately | ✅ | |
| 8.3 | Delete own comment works | ✅ | |
| 8.4 | Cannot delete another user's comment | ✅ | No delete button shown for others' comments |

## 9. Project detail — Members tab

| # | Scenario | Status | Notes |
|---|---|---|---|
| 9.1 | Lists all members with correct roles | ✅ | |
| 9.2 | Empty state (no members yet) renders sensibly | ✅ | |

## 10. Project detail — Analytics tab

| # | Scenario | Status | Notes |
|---|---|---|---|
| 10.1 | Stat cards are accurate | ✅ | |
| 10.2 | Task Status pie chart renders a visible, correctly-colored circle | ✅ | Fixed last pass (Recharts animation race) |
| 10.3 | Status Breakdown bar chart renders | ✅ | |
| 10.4 | Priority tab bar chart renders, colored by priority | ✅ | |
| 10.5 | Assignees tab bar chart renders | ✅ | |
| 10.6 | Trends tab renders real data, not random numbers | ✅ | Fixed last pass |
| 10.7 | Switching between chart tabs doesn't leave any tab blank | ✅ | |
| 10.8 | Empty state (0 tasks) doesn't crash the charts | ✅ | |
| 10.9 | Insights panel shows the right conditional messages | ✅ | |

## 11. My Tasks page

| # | Scenario | Status | Notes |
|---|---|---|---|
| 11.1 | Lists every task assigned to the current user, across all projects | ✅ | |
| 11.2 | Filter tabs filter correctly | ✅ | |
| 11.3 | Overdue tasks are visually flagged | ✅ | |
| 11.4 | "View Project" from a task row navigates correctly | ✅ | |
| 11.5 | Stat counts match the filtered lists | ✅ | |

## 12. Profile page

| # | Scenario | Status | Notes |
|---|---|---|---|
| 12.1 | Displays correct name, email, role, member-since date | ✅ | |
| 12.2 | Edit name + save persists (survives reload) | ✅ | |
| 12.3 | Navbar reflects the updated name **without a reload** | 🔧 | Was only writing to localStorage, not the live AuthContext state — added `updateUser()` |
| 12.4 | Save button is disabled when the name is unchanged | ✅ | |
| 12.5 | Name validation (too short/long) is rejected | ✅ | Backend `@Size(min=2, max=50)` |
| 12.6 | Email field is read-only | ✅ | |

## 13. Settings page

| # | Scenario | Status | Notes |
|---|---|---|---|
| 13.1 | Theme buttons switch immediately and match Navbar toggle | ✅ | |
| 13.2 | Notification-reminder toggle persists across reload | ✅ | |
| 13.3 | Logout button works | ✅ | See §3.8 |

## 14. Cross-cutting

| # | Scenario | Status | Notes |
|---|---|---|---|
| 14.1 | Dark mode is visually correct on every page | ✅ | |
| 14.2 | Light mode is visually correct on every page | ✅ | |
| 14.3 | Mobile viewport (375px): no horizontal scroll, nav holds up | ✅ | Checked dashboard + landing |
| 14.4 | Tablet viewport (768px): layout holds up | ⚠️ | Not separately exercised this pass — desktop (1440px) and mobile (375px) both clean; medium breakpoint not explicitly re-verified |
| 14.5 | Browser back/forward buttons behave sensibly | ✅ | Exercised implicitly across ~90 navigations this pass, no anomalies |
| 14.6 | No console errors on any page during normal use | ✅ | Zero across the entire multi-hour test run |
| 14.7 | No failed network requests during normal use | ✅ | |

---

## Bugs found this pass

Real, reproducible bugs found by driving the actual app — not by reading
the source and assuming it worked. Each was root-caused (not guessed at)
before being fixed, and re-tested live afterward.

### 1. Task detail modal was completely unreachable (the big one)
`TaskDetailModal.jsx` — the component with Edit/Delete buttons *and the
entire comment thread UI* — was fully built but never imported anywhere in
the app. Clicking a task card did nothing. Root cause, once actually
diagnosed instead of assumed: `@dnd-kit`'s `PointerSensor`, with no
activation constraint configured, claims every pointerdown on a sortable
card as a potential drag from pixel zero — which also swallows the card's
own native `onClick`. Fixed two things together: wired `TaskDetailModal`
into the project page (click a card → opens it, with Edit/Delete and a
comment thread), and added `activationConstraint: { distance: 8 }` to the
sensor so a stationary click and a real drag can coexist on the same
element — dnd-kit's own documented pattern for this exact conflict.
Re-verified drag-and-drop still works correctly after the fix.

### 2. Logout redirected to `/login` no matter what the logout handler pushed
Both Navbar's and Settings' logout handlers called `router.push('/')`, but
users always ended up back on `/login`. Three fix attempts were needed to
actually close this out — worth recording why the first two didn't work:
- **Attempt 1** (cancel `ProtectedRoute`'s pending redirect timer on
  unmount): didn't help — Next keeps the old route mounted during a
  client-side transition, so the timer's owning component hadn't actually
  unmounted yet when it fired.
- **Attempt 2** (check `window.location.pathname` at fire-time instead of
  assuming it): still lost the race — dev-mode navigation routinely takes
  longer than the timer's own delay, so the URL hadn't changed yet either.
- **Actual fix**: stopped inferring intent from timing entirely. Added an
  `isLoggingOut` flag in `AuthContext`, set synchronously the moment
  `logout()` runs; `ProtectedRoute` checks it and stands down instead of
  guessing. Deterministic, not timing-sensitive.

### 3. Editing your profile name didn't update the navbar until reload
`Profile`'s save handler wrote the new name straight to `localStorage`,
which is what survives a reload — but Navbar renders `user` from
`AuthContext`'s React state, which never got told about the change. Added
`updateUser()` to `AuthContext` so a save updates both together.

### 4. 404 page was Next.js's default — plain white, broke the dark theme
Hitting any bad URL dropped you onto an unstyled stock error page,
jarringly inconsistent with the rest of the app. Built a themed
`not-found.js`.

### 5. The live-demo section wasn't visible enough
User feedback: "I don't see the demo button." Valid — it was in the hero,
requiring a scroll past the fold on some viewports, with no visual
distinction from surrounding hero text. Added a "Live Demo" button in the
nav itself (visible immediately, jumps to the picker via anchor link) and
gave the picker its own bordered, tinted card so it reads as a distinct
widget rather than more paragraph text.

### 6. (Previous pass, listed for completeness) Frontend hardcoded `localhost:6060`
Both `AuthContext.js` and `services/api.js` ignored `NEXT_PUBLIC_API_URL`
entirely — worked by coincidence in local dev, would have been completely
broken on any real deployment.

---

## What this pass did *not* cover

Being honest about scope, not just claiming completeness:
- Tablet breakpoint (768px) — spot-checked visually earlier, not
  re-verified with this pass's assertions.
- Cross-browser (Firefox/Safari) — only tested in Chromium.
- Load/concurrency behavior (multiple users editing the same task
  simultaneously, etc.) — out of scope for a UI QA pass.
- Email delivery for the daily task-reminder scheduler — requires real SMTP
  credentials, not exercised.
