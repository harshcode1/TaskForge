# TaskForge Backend

## Project Description
TaskForge is a project management tool designed to streamline collaboration and task tracking for teams. The backend is built using Java and Spring Boot, providing robust APIs for managing projects, tasks, comments, and user authentication.

## Features
- **User Authentication**: Secure login and registration using JWT.
- **Project Management**: Create, update, and delete projects.
- **Task Management**: Assign tasks to projects, update task statuses, and manage priorities.
- **Comments**: Add and manage comments on tasks.
- **Dashboard**: View aggregated project and task data.
- **Role-Based Access Control**: Manage user roles and permissions.
- **Swagger Integration**: API documentation and testing.

## Architecture
The backend follows a layered architecture:
- **Controller Layer**: Handles HTTP requests and responses.
- **Service Layer**: Contains business logic.
- **Repository Layer**: Interacts with the database using JPA.
- **Entity Layer**: Defines database models.
- **DTO Layer**: Transfers data between layers.
- **Security Layer**: Manages authentication and authorization.

## Setup Instructions
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-repo/taskforge-backend.git
   cd taskforge-backend
   ```

2. **Install Dependencies**:
   Ensure you have Maven installed, then run:
   ```bash
   mvn install
   ```

3. **Configure Database**:
   Update `application.properties` with your database credentials.

4. **Run the Application**:
   ```bash
   mvn spring-boot:run
   ```

5. **Access Swagger UI**:
   Navigate to `http://localhost:8080/swagger-ui.html` to explore the API.

## Usage
### Swagger Endpoints
Below are some key endpoints:

#### Authentication
- **POST** `/api/auth/login`: Authenticate a user.
- **POST** `/api/auth/register`: Register a new user.

#### Projects
- **POST** `/api/projects`: Create a new project.
- **GET** `/api/projects/{id}`: Get project details.
- **DELETE** `/api/projects/{id}`: Delete a project.

#### Tasks
- **POST** `/api/tasks`: Create a new task.
- **GET** `/api/tasks?projectId={id}`: Get tasks for a project.

#### Comments
- **POST** `/api/comments`: Add a comment to a task.
- **DELETE** `/api/comments/{id}`: Delete a comment.

## Contributing
Contributions are welcome! Please fork the repository and submit a pull request.

## License
This project is licensed under the MIT License. See the LICENSE file for details.
