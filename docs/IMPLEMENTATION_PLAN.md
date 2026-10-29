# CivicConnect implementation plan

## Scope of this foundation

This phase establishes a separately deployable Spring Boot REST API and React web application, secure configuration conventions, and the design needed for incremental development. It deliberately does **not** provide registration, authentication, persistence entities, business workflows, or dashboards yet.

## Architecture

The repository uses a client/server layout:

```text
React (Vite) browser client
        | HTTPS / JSON REST
Spring MVC controllers -> service layer -> Spring Data JPA repositories -> MySQL
        |                         |
    DTO validation              Hibernate entities
        |
Spring Security authorization and exception handling
```

Backend packages will be grouped under `com.civicconnect.api`:

| Layer | Responsibility |
| --- | --- |
| `controller` | Versioned REST endpoints, HTTP status codes, request validation and response DTOs. |
| `service` | Transactional workflow and business rules; controllers do not access repositories directly. |
| `repository` | Spring Data JPA query interfaces. |
| `entity` | Hibernate/JPA persistence mappings only. |
| `dto` | Separate request/response shapes to avoid exposing entities. |
| `security` | Authentication, password hashing, JWT/filter support, principal and role/ownership authorization. |
| `config` | Security, CORS, application configuration and error handling. |

The frontend will add feature folders (`auth`, `grievances`, `admin`, `officer`) and a shared API client. It will never be treated as the authority for access control; every permission decision remains enforced by the backend.

## Roles and permissions

| Role | Permitted future actions |
| --- | --- |
| `CITIZEN` | Manage own profile; create grievances; view only own grievances, tracking IDs, status history and related notifications. |
| `OFFICER` | View only grievances assigned to the officer or their department; update allowed progress/status; add resolution notes. |
| `ADMIN` | Manage officers/departments; view all grievances; assign/reassign work; set priority; review analytics and audit information. |

Passwords will be stored only as BCrypt hashes. Authentication and ownership checks will be centralized in Spring Security and service methods. Tokens, credentials and role claims will not be trusted solely because the frontend sends them.

## Planned data model

| Entity | Purpose and principal relationships |
| --- | --- |
| `User` | Account identity, password hash, name/contact details and role. One citizen creates many grievances; an officer receives assignments. |
| `Department` | Civic department (for example, roads or sanitation). One department has many officers and receives many grievance assignments. |
| `Grievance` | Core report: generated tracking ID, category, description, location, priority and current status. Created by one citizen; has many assignments/status-history entries/notifications. |
| `GrievanceAssignment` | Assignment record joining one grievance to a department and optionally an officer. Retains assignment/reassignment history. |
| `GrievanceStatusHistory` | Immutable status transition with previous/new status, actor, timestamp and optional note; belongs to one grievance. |
| `Notification` | In-app delivery record for a user concerning a grievance; belongs to one user and optionally one grievance. |
| `AuditLog` | Immutable admin/security-sensitive activity record, actor, action, entity reference, timestamp and contextual metadata. |

Enums will define `Role`, `GrievanceStatus`, `Priority`, `Category`, and notification type. Referential integrity, indexes for tracking ID/status/assignee, UTC timestamps, and database constraints will be defined in Flyway migrations. The `mysql` profile uses `ddl-auto=validate`, so Hibernate validates migrations rather than altering production schemas.

## Planned API groups

All endpoints will be rooted at `/api`; detailed request/response schemas arrive with each feature phase.

| Group | Planned endpoint examples |
| --- | --- |
| Authentication | `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout` |
| Citizens | `GET/PATCH /citizens/me`, `GET /citizens/me/grievances`, `GET /citizens/me/notifications` |
| Grievances | `POST /grievances`, `GET /grievances/{trackingId}`, `GET /grievances/{id}/history`, `POST /grievances/{id}/attachments` |
| Departments | `GET /departments`, admin-only `POST/PATCH /departments` |
| Officers | `GET /officers/me/assignments`, `PATCH /officers/assignments/{id}/status`, `POST /officers/assignments/{id}/resolution-notes` |
| Administration | `GET /admin/grievances`, `POST /admin/grievances/{id}/assignments`, `PATCH /admin/grievances/{id}/priority`, officer management endpoints |
| Analytics | `GET /analytics/overview`, `GET /analytics/departments`, `GET /analytics/resolution-times` (admin-only) |

The only present endpoint is `GET /api/health`, intentionally public for a deployment check. All other routes are denied until a feature explicitly enables and secures them.

## Implementation phases

1. **Foundation (current):** repository layout, configuration, health endpoint, Vite landing page, project plan.
2. **Identity and database baseline:** Flyway initial schema, user/department model, registration/login, BCrypt password storage, role-based security.
3. **Citizen reporting:** grievance creation, generated tracking ID, validation, ownership queries and status viewing.
4. **Officer workflow:** assignment, controlled status transitions and resolution notes/history.
5. **Administration and communication:** department/officer management, priorities, notifications and audit logging.
6. **Analytics and hardening:** reporting queries, pagination/filtering, error handling, CORS, rate/security review and deployment configuration.

## Testing strategy

- Unit test service rules with JUnit and Mockito.
- Use Spring MVC/MockMvc tests for validation, response contracts and authorization boundaries.
- Add repository integration tests against an isolated MySQL-compatible test database when persistence begins.
- Add Flyway migration tests to verify a clean database can be migrated.
- Add React component tests and API-client tests as UI features are introduced.
- Exercise critical end-to-end paths: citizen report, assignment, officer resolution, citizen status visibility and admin oversight.
