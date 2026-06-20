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
- Invite team members with role-based access (Admin / Manager / Member)
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
- Invite members by email; manage roles per project

### Personal Workspace
- **My Tasks** page — see every task assigned to you across all projects
- **Dashboard** — personal stats, completion rate, and recent activity

### Developer Experience
- REST API with 18 endpoints, documented via **Swagger UI**
- JWT authentication — stateless, secure, 24-hour token expiry
- 57 frontend unit tests (Jest + React Testing Library)
- 12 backend test files (JUnit 5 + Mockito)
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
| Spring Mail | — | Scheduled email reminders |
| Springdoc OpenAPI | 2.8.8 | Swagger UI auto-generation |
| JUnit 5 + Mockito | — | Unit and integration tests |
| Maven | 3.x | Build tool |

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| Next.js | 14.2.30 | App Router, SSR, file-based routing |
| React | 18 | Component model |
| TailwindCSS | 3.4 | Utility-first styling |
| Axios | 1.10 | HTTP client with interceptors |
| Radix UI (9 pkgs) | various | Accessible headless UI components |
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
│              (React 18 · TailwindCSS · Axios)                   │
│                       Port 3000                                 │
└───────────────────────────┬─────────────────────────────────────┘
                            │  HTTP/JSON
                            │  Authorization: Bearer <jwt>
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot 3.5.3                            │
│          JwtAuthFilter → Controller → Service → Repository      │
│                       Port 6060                                 │
│              Swagger UI: /swagger-ui.html                       │
└───────────────────────────┬─────────────────────────────────────┘
                            │  JDBC / JPA
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
4. Service layer enforces authorization rules (owner-only, member-only, author-only)
5. Response interceptor auto-redirects to `/login` on any 401

---

## Project Structure

```
TaskForge/
├── backend/                          Spring Boot REST API
│   ├── src/main/java/.../
│   │   ├── controller/               6 controllers, 18 endpoints
│   │   ├── service/                  Business logic + authorization
│   │   ├── repository/               Spring Data JPA repositories
│   │   ├── entity/                   JPA entities (User, Project, Task, Comment, ProjectMember)
│   │   ├── dto/                      Request/Response DTOs
│   │   ├── security/                 JWT filter, JwtUtil, UserDetailsService
│   │   ├── exception/                Global exception handler
│   │   └── config/                   SecurityConfig, MailConfig, SwaggerConfig
│   ├── src/test/                     12 test files (unit + integration)
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                         Next.js 14 App Router
│   ├── src/
│   │   ├── app/                      6 routes: /, /login, /register, /dashboard, /projects, /my-tasks
│   │   ├── components/               10 feature components + 15 shadcn/ui components
│   │   ├── contexts/AuthContext.js   Global auth state
│   │   └── services/api.js           Axios instance + 26 API functions
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
| PUT | `/api/tasks/{id}` | Owner/Manager | Update task |
| DELETE | `/api/tasks/{id}` | Owner/Manager | Delete task |
| GET | `/api/comments/task/{id}` | Required | Comments on a task |
| POST | `/api/comments/add` | Required | Add a comment |
| DELETE | `/api/comments/{id}` | Author only | Delete a comment |
| POST | `/api/project-members/invite` | Owner only | Invite a member |
| GET | `/api/project-members/{id}` | Required | Project members list |
| GET | `/api/dashboard/{projectId}` | Required | Project analytics |
| GET | `/api/dashboard/my-dashboard` | Required | Personal task stats |

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
| `api.test.js` | 20 | All 26 API functions, auth interceptors |
| `CommentSection.test.jsx` | 11 | Field mapping, delete auth, optimistic UI |
| `KanbanBoard.test.jsx` | 7 | 4 columns, task placement, PENDING status |
| `TaskModal.test.jsx` | 12 | Status options, validation, create/edit modes |
| `ProjectsPage.test.jsx` | 7 | Edit dialog, API call, local state update |

### Backend — 12 test files
```bash
cd backend
./mvnw test
```

| Layer | Files | What it covers |
|---|---|---|
| Service (unit) | 6 files | Business logic, authorization rules, edge cases |
| Controller (integration) | 6 files | HTTP layer, auth, 401 enforcement |

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

ProjectMember: User ↔ Project  (role: ADMIN | MANAGER | MEMBER)
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
| `JWT_SECRET` | Yes | Secret key for JWT signing (min 32 chars) |
| `JWT_EXPIRATION` | No | Token expiry in ms (default: 86400000 = 24h) |
| `MAIL_HOST` | No | SMTP host (default: smtp.gmail.com) |
| `MAIL_PORT` | No | SMTP port (default: 587) |
| `MAIL_USERNAME` | No | Email for sending reminders |
| `MAIL_PASSWORD` | No | Email app password |

---

## Key Design Decisions

- **Monorepo** — Frontend and backend in one repo simplifies cloning, CI/CD, and environment management
- **Stateless JWT auth** — No server-side sessions; tokens stored client-side with 24h expiry and auto-redirect on 401
- **Optimistic UI** — Comments and project edits update the UI before the server confirms, then reconcile on error
- **Parallel data fetching** — Dashboard loads projects, personal stats, and tasks in a single `Promise.all()` — not a sequential loop
- **Role-based authorization** — Enforced at the service layer (not just controller): owner-only for project edits, author-only for comment deletes, manager+ for task edits

---

## Author

**Harsh Soni**  
[GitHub](https://github.com/harshcode1) · [LinkedIn](https://linkedin.com/in/harshsoni9995)

---

<div align="center">
  <sub>Built with Spring Boot · Next.js · MySQL · Docker</sub>
</div>
