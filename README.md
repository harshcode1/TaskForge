# TaskForge

Full-stack project management tool. Spring Boot backend + Next.js frontend in a single monorepo.

## Stack

| Layer    | Technology                          | Port |
|----------|-------------------------------------|------|
| Backend  | Spring Boot 3.5.3, Java 17, MySQL 8 | 6060 |
| Frontend | Next.js 14, React 18, TailwindCSS   | 3000 |
| Auth     | JWT (HS256, 24h expiry)             | —    |

## Quick Start (Docker)

```bash
# 1. Copy env file and fill in secrets
cp .env.example .env

# 2. Start everything
docker-compose up --build

# Frontend → http://localhost:3000
# Backend  → http://localhost:6060
# API Docs → http://localhost:6060/swagger-ui.html
```

## Local Development

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Requires MySQL running on port 3306 with database `projectmanagementdb`.  
Set env vars or edit `src/main/resources/application.properties`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:3000`. Talks to backend at `http://localhost:6060/api`.

### Tests

```bash
# Frontend unit tests
cd frontend
npm test

# Backend tests
cd backend
./mvnw test
```

## Project Structure

```
TaskForge/
├── backend/          Spring Boot API (18 endpoints)
│   ├── src/
│   └── pom.xml
├── frontend/         Next.js app
│   ├── src/
│   └── package.json
├── docker-compose.yml
├── .env.example
└── README.md
```

## Features

- JWT authentication (register / login)
- Project management (create, edit, delete)
- Kanban board with drag-and-drop (TODO / IN_PROGRESS / PENDING / DONE)
- Task assignment, priorities, due dates
- Comments with delete
- Project analytics dashboard
- My Tasks personal view
- Daily email reminders for overdue tasks
- Dark / light mode
