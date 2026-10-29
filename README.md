# CivicConnect

CivicConnect is a public grievance reporting and resolution management system. Citizens will report civic issues such as potholes, garbage, water-supply faults, drainage problems and broken streetlights; officers will manage assigned work; and administrators will supervise departments, priorities and outcomes.

This repository currently contains the **foundation only**: a runnable health endpoint, a React landing page, configuration structure and an implementation plan. Authentication, database entities and grievance workflows are intentionally planned but not yet implemented.

## Technology stack

- Java 21, Spring Boot 3.5, Spring MVC and REST APIs
- Spring Data JPA / Hibernate with MySQL
- Spring Security
- Maven
- React, JavaScript, HTML and CSS using Vite
- Flyway for safe, versioned database migrations in the MySQL profile

## Prerequisites

- JDK 21
- Apache Maven 3.9+
- Node.js 20.19+ (or a current Node.js LTS) and npm
- MySQL 8+ is needed only when starting the future persistence-enabled `mysql` profile

## Planned setup

1. Copy `.env.example` to a local, ignored `.env` file as a private reference for your connection values. This foundation deliberately does not load `.env` files; export those values in your shell or configure them in your IDE's run configuration.
2. MySQL-backed development must export `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, and a random `JWT_SECRET` of at least 32 bytes, then use the `mysql` Spring profile. `JWT_EXPIRATION_SECONDS` is optional and defaults to 3600.
3. Database schema changes will be committed as Flyway migrations; do not use `ddl-auto=update`.

## Run the current foundation on macOS

In one Terminal:

```zsh
cd /Users/koushalbalimane/Desktop/CivicConnect/backend
mvn spring-boot:run
```

Then check [http://localhost:8080/api/health](http://localhost:8080/api/health). The default `local` profile deliberately runs without MySQL because persistence has not started.

In another Terminal:

```zsh
cd /Users/koushalbalimane/Desktop/CivicConnect/frontend
npm install
npm run dev
```

Open the local URL Vite prints (normally [http://localhost:5173](http://localhost:5173)).

When database support is implemented, an example MySQL-profile invocation will be:

```zsh
cd /Users/koushalbalimane/Desktop/CivicConnect/backend
export SPRING_PROFILES_ACTIVE=mysql
export DB_HOST=localhost DB_PORT=3306 DB_NAME=civicconnect
export DB_USERNAME=your_mysql_username DB_PASSWORD=your_mysql_password
export JWT_SECRET=replace_with_a_random_32_byte_or_longer_secret
mvn spring-boot:run
```

See [the implementation plan](docs/IMPLEMENTATION_PLAN.md) for the proposed design, entity relationships, API groups, roles, phases and testing approach. See [the MySQL and Flyway setup guide](docs/DATABASE_SETUP.md) to initialize the local database and run migration version 1.
