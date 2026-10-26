# SkillForge

SkillForge is a student collaboration web application developed for the **CS1207 Object-Oriented Programming Lab**. It is designed to help students create and manage projects, discover teammates, collaborate through project invitations and tasks, and maintain a personal portfolio.

## Current Features

- User registration and login
- Secure session-based authentication
- Student profile management
- Student discovery/search
- Project creation and project membership
- Project ownership and member roles
- Project invitations with accept/reject flow
- Invitation expiry and team-capacity checks
- Project task creation and editing
- Task assignment to project members
- Task status updates (`TODO`, `IN_PROGRESS`, `DONE`)
- Personal portfolio items
- Optional portfolio links to projects
- Shared authenticated navigation and consistent UI styling
- Server-side authorization for protected project, task, invitation, profile, and portfolio operations
- Form validation and safe error handling

## Technology Stack

- **Java 21**
- **Spring Boot 4.1.1**
- **Spring MVC**
- **Thymeleaf**
- **Spring Data JPA / Hibernate**
- **Spring Security**
- **Jakarta Bean Validation**
- **MySQL**
- **Maven**
- **JUnit / Spring Boot Test / MockMvc**

## Architecture

SkillForge follows a layered Spring application structure:

```text
Browser
   |
   v
Thymeleaf Views
   |
   v
Controllers
   |
   v
Services
   |
   v
Repositories
   |
   v
JPA Entities
   |
   v
MySQL Database
```

Security is handled through Spring Security and authenticated sessions. Authorization decisions are enforced on the server rather than trusting user-supplied identifiers from the browser.

## Main Domain Areas

The application currently works around the following core entities and concepts:

- `AppUser`
- `StudentProfile`
- `Skill`
- `StudentSkill`
- `Project`
- `ProjectMember`
- `ProjectInvitation`
- `Task`
- `PortfolioItem`
- `Achievement`

Project membership uses owner/member roles, while task status uses `TODO`, `IN_PROGRESS`, and `DONE`.

## Project Structure

```text
src/
├── main/
│   ├── java/com/skillforge/skillforge/
│   │   ├── controller / web controllers
│   │   ├── dto        / request and response objects
│   │   ├── entity     / JPA entities
│   │   ├── enums      / domain enums
│   │   ├── repository / Spring Data repositories
│   │   ├── security   / authentication and authorization support
│   │   └── service    / application and business logic
│   │
│   └── resources/
│       ├── templates/ / Thymeleaf pages
│       └── static/    / CSS and other static assets
│
└── test/
    └── java/          / automated tests
```

## Running the Application

### Requirements

Install:

- Java 21
- MySQL 8.x
- Git

### 1. Clone the repository

```bash
git clone https://github.com/manishkotha04/SkillForge.git
cd SkillForge
```

### 2. Configure MySQL

Create a MySQL database for the application and configure the database connection using the project's Spring configuration/environment variables.

Do **not** commit database passwords, secrets, or other private credentials to Git.

### 3. Build and test

On Windows:

```powershell
./mvnw.cmd clean test
```

On macOS/Linux:

```bash
./mvnw clean test
```

### 4. Run the application

Windows:

```powershell
./mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

Then open the application in a browser using the configured local server address.

## Important Application Flows

### Authentication

```text
Register
  -> Login
  -> Authenticated session
  -> Dashboard
```

### Project Collaboration

```text
Create Project
  -> Project Owner
  -> Find Teammates / Invite User
  -> Invitation
  -> Accept / Reject
  -> Project Membership
```

### Task Management

```text
Project
  -> Create Task
  -> Assign Member (optional)
  -> TODO
  -> IN_PROGRESS
  -> DONE
```

### Portfolio

```text
My Portfolio
  -> Add Portfolio Item
  -> Optional Project Link
  -> View / Edit Portfolio Item
```

## Security Notes

SkillForge uses Spring Security for authenticated access and BCrypt-based password hashing. Protected operations perform server-side ownership or membership checks.

The application is designed so that users cannot simply change a browser parameter to operate on another user's private profile or portfolio data, and project task assignment is limited to project members.

## Development Workflow

The project uses Git feature branches for team development. Changes should be made in the appropriate feature branch, tested locally, and integrated through the team's agreed Git workflow.

Before pushing a completed change, run:

```bash
./mvnw clean test
```

For Windows:

```powershell
./mvnw.cmd clean test
```

## Academic Context

This project is developed as part of the **CS1207 Object-Oriented Programming Lab**. The implementation and documentation are being developed alongside the team's Software Requirements Specification (SRS).

The README describes the implemented application scope and is not intended to replace the project's official SRS or faculty documentation.

## Repository

**GitHub:** https://github.com/manishkotha04/SkillForge
