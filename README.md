<div align="center">

# TaskForge

**A full-stack project management platform built for teams.**  
Organize projects, assign tasks, track progress on a Kanban board, and collaborate with comments — all in one place.

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.3-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-14-black?style=flat-square&logo=next.js)](https://nextjs.org/)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react)](https://react.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)

[Features](#features) · [Tech Stack](#tech-stack) · [Quick Start](#quick-start) · [API Docs](#api-documentation) · [Architecture](#architecture) · [Tests](#running-tests)

</div>

---

## What is TaskForge?

TaskForge is a **full-stack project management tool** inspired by tools like Jira and Trello. It lets teams create projects, invite members, create and assign tasks with priorities and due dates, visualize work on a drag-and-drop Kanban board, and collaborate through task comments.

Built as a monorepo with a **Spring Boot REST API** (backend) and a **Next.js App Router** application (frontend), communicating over JWT-authenticated HTTP.

---

## Features

### Project Management
- Create, edit, and delete projects
- Invite team members as Manager or Member; the project's creator is its owner
- Per-project Kanban board, analytics, and member directory
- Project completion analytics with charts

### Task Management
- Create tasks with title, description, priority (Low / Medium / High), status, due date, and assignee
- Four status lanes: **To Do**, **In Progress**, **Pending**, **Done**
- Drag and drop tasks between columns to update status instantly
- Filter tasks by status with live count badges
- Overdue detection — tasks past their due date are flagged in red

### Collaboration
- Add comments to any task
- Delete your own comments
- Invite members by email as Manager or Member, and remove them (owner only)

### Live Updates and History
- **Live board** — when a teammate creates, edits or deletes a task, your open board updates without a refresh (WebSocket / STOMP, project members only)
- **Activity feed** — a saved history of task changes for each project
- **⌘K command palette** — jump to any page, project or task from the keyboard
- **AI helpers (optional)** — generate a task description from its title and a plain-English project summary; they switch on only when `OPENAI_API_KEY` is set

### Personal Workspace
- **My Tasks** page — see every task assigned to you across all projects
- **Dashboard** — personal stats, completion rate, and recent activity

### Developer Experience
- REST API with 26 endpoints, documented via **Swagger UI**
- JWT authentication — stateless, secure, 24-hour token expiry
- 57 frontend unit tests (Jest + React Testing Library)
- 183 backend tests (JUnit 5 + Mockito, plus integration tests that call the real API with real JWTs on an in-memory H2 database)
- One-command Docker Compose setup
- Dark / Light mode with system preference detection

---

## Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Language (LTS) |
| Spring Boot | 3.5.3 | Framework, embedded Tomcat |
| Spring Security | 6 | Stateless JWT filter chain |
| Spring Data JPA | 3.x | ORM, repository layer |
| Hibernate | 6 | JPA implementation |
| MySQL | 8.0 | Relational database |
| JJWT | 0.11.5 | JWT generation and validation (HS256) |
| BCrypt | — | Password hashing |
| Spring WebSocket (STOMP) | — | Live board updates (simple in-memory broker) |
| Flyway | — | Versioned schema migrations (production) |
| Spring Mail | — | Scheduled email reminders |
| Springdoc OpenAPI | 2.1.0 | Swagger UI auto-generation |
| JUnit 5 + Mockito + H2 | — | Unit and integration tests (H2 = in-memory test database) |
| Maven | 3.x | Build tool |

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| Next.js | 14.2.35 | App Router, SSR, file-based routing |
| React | 18 | Component model |
| TailwindCSS | 3.4 | Utility-first styling |
| Axios | 1.20 | HTTP client with interceptors |
| @stomp/stompjs | 7.3 | STOMP client for live updates |
| cmdk | 1.1 | ⌘K command palette |
| Radix UI (10 pkgs) | various | Accessible headless UI components |
| shadcn/ui | — | Pre-built component library over Radix |
| @dnd-kit | 6/10/3 | Drag-and-drop Kanban board |
| Recharts | 3.1 | Analytics charts |
| lucide-react | 0.525 | SVG icon library |
| next-themes | 0.4.6 | Dark/light mode |
| react-hot-toast | 2.5.2 | Toast notifications |
| Jest 29 + RTL | — | Unit testing |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Browser                                  │
│                   Next.js 14 App Router                         │
│        (React 18 · TailwindCSS · Axios · STOMP client)          │
│                       Port 3000                                 │
└───────────────────────────┬───────────────────┬─────────────────┘
                            │ HTTP/JSON         │ WebSocket (STOMP)
                            │ Bearer <jwt>      │ JWT on CONNECT, /ws
                            ▼                   ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot 3.5.3                            │
│   REST:  JwtAuthFilter → Controller → Service → Repository      │
│   Live:  STOMP interceptor → /topic/project/{id}/tasks          │
│                       Port 6060                                 │
│              Swagger UI: /swagger-ui.html                       │
└───────────────────────────┬─────────────────────────────────────┘
                            │  JDBC / JPA (schema owned by Flyway in production)
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MySQL 8.0                                    │
│              Database: projectmanagementdb                      │
│                       Port 3306                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Request lifecycle:**
1. Axios request interceptor attaches `Authorization: Bearer <token>` from localStorage
2. Spring's `JwtAuthFilter` validates the token on every request
3. Controller extracts the authenticated user via `@AuthenticationPrincipal`
4. Service layer enforces authorization through one helper, `ProjectAccessService` (owner / manager / member checks; a non-member gets 404, a member without the right gets 403)
5. Response interceptor auto-redirects to `/login` on any 401
6. After a task is created, updated or deleted, the server also pushes a small event to `/topic/project/{id}/tasks` (live board) and saves an activity-log row

---

## Project Structure

```
TaskForge/
├── backend/                          Spring Boot REST API
│   ├── src/main/java/.../
│   │   ├── controller/               9 controllers, 26 endpoints
│   │   ├── service/                  Business logic + authorization
│   │   ├── repository/               Spring Data JPA repositories
│   │   ├── entity/                   JPA entities (User, Project, Task, Comment, ProjectMember, ActivityLog)
│   │   ├── dto/                      Request/Response DTOs
│   │   ├── security/                 JWT filter, JwtUtil, UserDetailsService, WebSocket (STOMP) interceptor
│   │   ├── exception/                Global exception handler
│   │   └── config/                   SecurityConfig, WebSocketConfig, MailConfig, SwaggerConfig, DemoDataSeeder
│   ├── src/main/resources/db/migration/   Flyway migrations V1–V5
│   ├── perf/                         Dev-only: data seeding + index measurement scripts and results
│   ├── src/test/                     25 test classes (unit + integration)
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                         Next.js 14 App Router
│   ├── src/
│   │   ├── app/                      9 routes + 404: /, /login, /register, /dashboard, /projects,
│   │   │                             /projects/[id], /my-tasks, /profile, /settings
│   │   ├── components/               13 feature components + 17 shadcn/ui components
│   │   ├── contexts/AuthContext.js   Global auth state
│   │   └── services/api.js           Axios instance + 25 API functions
│   ├── src/__tests__/                5 test suites, 57 tests
│   ├── Dockerfile
│   └── package.json
│
├── docker-compose.yml                Runs db + backend + frontend together
├── .env.example                      All required environment variables
└── README.md
```

---

## Quick Start

### Option 1 — Docker (Recommended)

Requires: [Docker Desktop](https://www.docker.com/products/docker-desktop/)

```bash
# Clone the repo
git clone https://github.com/harshcode1/TaskForge.git
cd TaskForge

# Set up environment variables
cp .env.example .env
# Edit .env and set a strong JWT_SECRET

# Start everything (MySQL + Backend + Frontend)
docker-compose up --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:6060 |
| Swagger UI | http://localhost:6060/swagger-ui.html |
| MySQL | localhost:3306 |

---

### Option 2 — Local Development

**Prerequisites:** Java 17+, Maven, Node.js 18+, MySQL 8

**1. Database setup**
```sql
CREATE DATABASE projectmanagementdb;
```

**2. Backend**
```bash
cd backend

# Set environment variables (or edit application.properties directly)
export DB_USERNAME=your_mysql_user
export DB_PASSWORD=your_mysql_password
export JWT_SECRET=your-secret-key-minimum-32-characters

./mvnw spring-boot:run
# API running at http://localhost:6060
```

**3. Frontend**
```bash
cd frontend
npm install
npm run dev
# App running at http://localhost:3000
```

---

## API Documentation

Full interactive API docs are available via Swagger UI when the backend is running:

```
http://localhost:6060/swagger-ui.html
```

### Endpoint Summary

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register and receive JWT |
| POST | `/api/auth/login` | Public | Login and receive JWT |
| GET | `/api/projects` | Required | All visible projects (owned + member) |
| POST | `/api/projects` | Required | Create a project |
| GET | `/api/projects/{id}` | Required | Project details |
| PUT | `/api/projects/{id}` | Owner only | Update project |
| DELETE | `/api/projects/{id}` | Owner only | Delete project |
| GET | `/api/tasks/project/{id}` | Required | Tasks for a project |
| GET | `/api/tasks` | Required | My assigned tasks |
| POST | `/api/tasks` | Required | Create a task |
| PUT | `/api/tasks/{id}` | Owner/Manager/Member | Update task |
| DELETE | `/api/tasks/{id}` | Owner/Manager | Delete task (a plain member gets 403) |
| GET | `/api/comments/task/{id}` | Required | Comments on a task |
| POST | `/api/comments/add` | Required | Add a comment |
| DELETE | `/api/comments/{id}` | Author only | Delete a comment |
| POST | `/api/project-members/invite` | Owner only | Invite a member |
| GET | `/api/project-members/{id}` | Required | Project members list |
| DELETE | `/api/project-members/{projectId}/{userId}` | Owner only | Remove a member |
| GET | `/api/dashboard/{projectId}` | Required | Project analytics |
| GET | `/api/dashboard/my-dashboard` | Required | Personal task stats |
| GET | `/api/users/me` | Required | Your profile |
| PUT | `/api/users/me` | Required | Update your name |
| GET | `/api/activity/{projectId}` | Owner or member | Last 50 task-change events for a project |
| GET | `/api/ai/status` | Public | Whether the optional AI features are enabled |
| POST | `/api/ai/task-description` | Required | Generate a task description from its title (needs `OPENAI_API_KEY`) |
| GET | `/api/ai/project-summary/{projectId}` | Owner or member | Plain-English project status summary (needs `OPENAI_API_KEY`) |

Plus the WebSocket endpoint `/ws` (STOMP): a JWT is required on connect, and only the owner and members of a project can subscribe to `/topic/project/{id}/tasks`.

Project, task, comment and member endpoints return **404** to someone who is neither owner nor member (the same response as for an id that does not exist), and **403** to a member who lacks the right for the action.

---

## Running Tests

### Frontend — 57 tests across 5 suites
```bash
cd frontend
npm install
npm test

# With coverage report
npm run test:coverage
```

| Suite | Tests | What it covers |
|---|---|---|
| `api.test.js` | 20 | API functions, auth interceptors |
| `CommentSection.test.jsx` | 11 | Field mapping, delete auth, add / remove |
| `KanbanBoard.test.jsx` | 7 | 4 columns, task placement, PENDING status |
| `TaskModal.test.jsx` | 12 | Status options, validation, create/edit modes |
| `ProjectsPage.test.jsx` | 7 | Edit dialog, API call, local state update |

### Backend — 183 tests (no database needed; integration tests use in-memory H2)
```bash
cd backend
./mvnw test
```

| Layer | Files | What it covers |
|---|---|---|
| Service + security (unit) | 8 files | Business logic, access rules (`ProjectAccessService`), STOMP interceptor |
| Controller / WebSocket / config (integration) | 17 files | Real JWTs through the real security chain: owner vs member vs non-member for every project/task endpoint, live-update subscription, registration, seeder flag |

---

## Data Model

```
User ──────────────────────────── owns many ──► Project
  │                                                 │
  │ assigned to many                         has many │
  ▼                                                 ▼
Task ◄──────────────── belongs to ──────── Project
  │
  └── has many ──► Comment ◄── authored by ── User

ProjectMember: User ↔ Project  (role: MANAGER | MEMBER, unique per user and project;
               the owner is Project.owner, not a row here)
TaskStatus:    TODO | IN_PROGRESS | PENDING | DONE
Priority:      LOW | MEDIUM | HIGH
```

---

## Environment Variables

Copy `.env.example` to `.env` and configure:

| Variable | Required | Description |
|---|---|---|
| `DB_USERNAME` | Yes | MySQL username |
| `DB_PASSWORD` | Yes | MySQL password |
| `DB_ROOT_PASSWORD` | Docker only | MySQL root password |
| `APP_SEED_DEMO_DATA` | No | `true` creates the demo accounts and sample data (default `false`; `docker-compose.yml` turns it on) |
| `JWT_SECRET` | Yes | Secret key for JWT signing (min 32 chars) |
| `JWT_EXPIRATION` | No | Token expiry in ms (default: 86400000 = 24h) |
| `MAIL_HOST` | No | SMTP host (default: smtp.gmail.com) |
| `MAIL_PORT` | No | SMTP port (default: 587) |
| `MAIL_USERNAME` | No | Email for sending reminders |
| `MAIL_PASSWORD` | No | Email app password |

---

## Technical Decisions

**Monorepo.** Frontend and backend live in one repository, so one clone, one `docker-compose.yml` and one `.env` cover the whole app.

**Why JWT.** The API is stateless: there is no session store, and the frontend just sends `Authorization: Bearer <token>`. Tokens are HS256-signed, carry the email as subject plus `role` and `userId` claims, and expire after `JWT_EXPIRATION` (default 24 h). The frontend keeps the token in `localStorage`, an Axios interceptor attaches it to every request, and any 401 clears it and redirects to `/login`. The trade-off is that a token cannot be revoked server-side: logging out only deletes it in the browser, so a stolen token works until it expires.

**Why STOMP over WebSocket.** The board should update when someone else changes a task. STOMP gives topics and subscriptions on top of a raw WebSocket, and Spring's built-in in-memory broker needs no extra infrastructure. The channel is broadcast-only: after a REST call succeeds, the server publishes a small event (`CREATED`/`UPDATED`/`DELETED`, task id and title, project id, who did it) to `/topic/project/{id}/tasks`, and other open boards refetch. The socket is secured by `StompAuthChannelInterceptor`: CONNECT must carry the JWT in the STOMP `Authorization` header (a browser cannot set headers on the WebSocket handshake itself, so `/ws` stays open at the HTTP level), SUBSCRIBE is allowed only to the topic of a project the user owns or belongs to, and clients may not SEND to `/topic/**` (otherwise anyone could forge events). Trade-off: the broker is in memory, so this works on a single backend instance; running several would need an external broker.

**Why Flyway.** Schema changes are versioned SQL instead of whatever Hibernate decides: `V1` baseline (captured from the Hibernate-generated schema), `V2` activity log, `V3` unique `(project_id, user_id)` on `project_members`, `V4` task index, `V5` drop of the index `V4` made redundant. The `prod` profile runs Flyway and sets `ddl-auto=validate`; `dev` uses `ddl-auto=update` with Flyway off; integration tests use H2 with `create-drop`. Trade-off: the migrations are MySQL SQL, so the H2 tests do not run them. They are checked by applying them to a real MySQL.

**Why membership is checked in the service layer.** The rule lives in one class, `ProjectAccessService` (`requireMember`, `requireManagerOrOwner`, `requireOwner`), and every service that touches a project, task or comment calls it. Endpoints that only take a task or comment id load it, find its project, then check. Doing it in services, not controllers, means the same rule protects REST and the WebSocket interceptor, and it is covered by fast unit tests. The owner is `Project.owner` (not a `project_members` row); `project_members` holds MANAGER and MEMBER only, and the API rejects ADMIN as a project role. The global `User.role` is stored and put in the token's authorities, but no permission decision uses it.

**404 vs 403.** Someone who is neither owner nor member gets **404** with the same body as a project, task or comment that does not exist (`"Not found"`), so ids cannot be probed for existence. A member who lacks the right for an action gets **403** (deleting a task needs owner or manager; updating or deleting a project and managing members need the owner; deleting a comment is author-only). Trade-off: a user cannot tell "no access" from "gone".

**Registration and the demo seeder.** Registering always creates a MEMBER; the request has no role field, so a body with `"role":"ADMIN"` is ignored. The demo seeder (three accounts with a published password, plus sample data) only exists when `app.seed-demo-data=true`. It defaults to `false`, `docker-compose.yml` turns it on for the live demo, and tests never enable it. Do not enable it on a real deployment.

**Parallel data fetching.** The dashboard loads projects, personal stats and my tasks in a single `Promise.all()` instead of sequential requests.

**Index on `tasks(project_id, status)` (`V4`).** The board loads `GET /api/tasks/project/{id}`, optionally with `?status=`. Hibernate runs it as `select ... from tasks t left join projects p on p.id = t.project_id where p.id = ? [and t.status = ?]`, with no `ORDER BY` and no paging. `tasks.project_id` already had a foreign-key index from `V1`, so the question was whether a composite index adds anything.

How it was measured: MySQL 8.0.46 in Docker on a laptop (Intel Core Ultra 7 155H, 15.6 GB RAM, Windows 11, Docker Desktop with WSL2), default 128 MB InnoDB buffer pool, warm cache. Data from `backend/perf/seed_tasks.sql`: 100,000 tasks in 1,000 projects, with one big project of 25,000 tasks and the rest about 75 each; statuses 40% TODO, 25% IN_PROGRESS, 10% PENDING, 25% DONE. `backend/perf/measure_task_list.py` runs each query 3 warm-up times and then 15 timed times, reading rows examined and server execution time (including sending the rows) from `performance_schema`, plus `EXPLAIN` and `EXPLAIN ANALYZE`. The experiment was run twice (before, after, then the index dropped and re-created for a second before and after) and the table shows both runs as first / second. Raw output is in `backend/perf/results-*.txt`.

| Query | Rows examined, before to after | Server time (median of 15), before to after |
|---|---|---|
| A. big project, all tasks (25,000 rows returned) | 25,000 to 25,000 | 49.2 / 45.2 ms to 49.4 / 49.6 ms |
| B. big project, `status=DONE` (6,250 rows returned) | 25,000 to 6,250 | 33.6 / 33.3 ms to 13.4 / 12.2 ms |
| C. small project (75 tasks), all tasks | 75 to 75 | 0.43 / 0.45 ms to 0.43 / 0.35 ms |
| D. small project, `status=DONE` (16 rows returned) | 75 to 16 | 0.39 / 0.36 ms to 0.27 / 0.26 ms |

`EXPLAIN ANALYZE` agrees (median of 7, top node, first round): B went from 30.8 ms to 8.6 ms, and D from 0.24 ms to 0.08 ms.

Result: the index helps only the status-filtered list, and clearly only for a large project: 4x fewer rows examined and about 2.5x less time for query B. It does **not** help the unfiltered list (A, C): the optimizer keeps using the old foreign-key index there, and every examined row is returned anyway. For ordinary small projects the difference (D, about 0.1 ms) is too small to matter. I kept the index because the board does filter by status and a project can grow, but it is a modest win, not a general speed-up. This index also serves lookups by `project_id` alone, which made the older single-column foreign-key index redundant; `V5` dropped it (next paragraph).

**Dropping the now-redundant index (`V5`).** After `V4`, the single-column foreign-key index on `tasks.project_id` from `V1` is a left prefix of `idx_tasks_project_status`, so `V5` drops it (it only costs disk and extra work on every task write). The foreign key itself stays: checked on MySQL 8 after the drop, `SHOW CREATE TABLE tasks` still lists the constraint, inserting a task for a missing project fails with error 1452, and deleting a project that still has tasks fails with 1451. The migration is guarded, so it does nothing on a database where that index does not exist under Hibernate's generated name. Flyway applied `V1`-`V5` to an empty MySQL in a full Docker Compose run, and the API worked afterwards.

Does dropping it slow anything down? Two plain before/after runs gave contradictory answers (+3.5% in one, +68% in the other for the big "all tasks" query), because the machine's speed drifts over minutes. So it was re-measured interleaved: 10 rounds alternating "with" and "without" the old index (order flipped each round), 30 timed runs per block, median per block, same 100,000-task data and 8.0.46 server. Median of block medians: **big project, all tasks 53.7 ms with the old index vs 50.8 ms without (-5.5%; without was at least as fast in 6 of 10 rounds)**; big project, `status=DONE` 11.8 ms vs 12.5 ms (+6.4%). The `status=DONE` query uses the composite index in both states, so its +6.4% is a measure of the noise: both differences are within it, and nothing got measurably slower. Rows examined are unchanged (25,000 and 6,250). Block-to-block spread was large (42 to 84 ms), so this shows "no measurable difference", not "faster". Raw output: `backend/perf/results-drop-fk-*.txt`, `compare_drop_fk_index.py`.

## Known Limitations

- **Removed members and their tasks.** Removing a member does not unassign their tasks. They still appear in that person's "My tasks" and personal dashboard, and updating such a task while re-sending the same assignee fails with 400 until someone reassigns it.
- **No paging or sorting** on the project task list; an unfiltered list for a huge project returns every task.
- **Demo defaults.** `docker-compose.yml` has a fallback JWT secret and default database passwords, and the demo accounts have a known password. This is for the demo only: set `JWT_SECRET` and `DB_*` in `.env` and leave `APP_SEED_DEMO_DATA` off for anything real.
- **JWT.** No server-side revocation (see above), and `JwtAuthFilter` still has duplicated and dead code that needs a cleanup.
- **WebSocket.** The in-memory broker means one backend instance only. With an expired or invalid token the server answers with a STOMP error and the browser client retries every 4 seconds until the REST client logs the user out.
- **Migrations are not unit-tested.** `V3`, `V4` and `V5` are MySQL SQL and the automated tests run on H2 without Flyway, so they were checked by applying them to a real MySQL.
- **Leftovers.** The `project_members.role` column from `V1` still lists `ADMIN` as an allowed value (the application never writes it), and the frontend has no "remove member" button yet (the endpoint exists).

## Author

**Harsh Soni**  
[GitHub](https://github.com/harshcode1) · [LinkedIn](https://linkedin.com/in/harshsoni9995)

---

<div align="center">
  <sub>Built with Spring Boot · Next.js · MySQL · Docker</sub>
</div>
