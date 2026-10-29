# MySQL and Flyway setup

## Scope and assumptions

`V1__create_initial_civicconnect_schema.sql` is the first CivicConnect migration. It assumes that the `civicconnect` database is empty. Flyway is configured not to baseline an unmanaged non-empty schema, preventing it from silently skipping version 1. Flyway owns the schema after this point; do not use Hibernate schema generation or manually alter these tables.

The initial model contains:

- `departments`: routing ownership for civic work.
- `users`: citizens, officers and administrators. An officer may belong to a department.
- `grievances`: a report from one citizen, optionally routed to a department and assigned officer.
- `grievance_status_history`: an append-only record of status changes, the actor and an optional note.

The database schema cannot enforce that a referenced `assigned_officer_id` has the `OFFICER` role; that cross-row business rule will be enforced in the future service and authorization layers.

## Start MySQL on macOS

For a Homebrew MySQL installation:

```zsh
brew services start mysql
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS civicconnect CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
```

If MySQL was installed another way, start its server through that installation and create the same database with an administrative account.

## Load local environment variables

The project-root `.env` is ignored by Git. It must contain local values for `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`; do not commit it. `JWT_SECRET` must be a random value of at least 32 bytes. `JWT_EXPIRATION_SECONDS` is optional and defaults to `3600`.

```zsh
cd /Users/koushalbalimane/Desktop/CivicConnect
set -a
source .env
set +a
export SPRING_PROFILES_ACTIVE=mysql
```

## Run the MySQL profile and migration

```zsh
cd /Users/koushalbalimane/Desktop/CivicConnect/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run
```

On a successful first startup, Flyway creates `flyway_schema_history` and applies version `1`; Hibernate then validates the migration-created schema because `ddl-auto` remains `validate`.

Verify the migration and tables with:

```zsh
MYSQL_PWD="$DB_PASSWORD" mysql \
  -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" "$DB_NAME" \
  -e "SELECT version, description, success FROM flyway_schema_history; SHOW TABLES;"
```

The existing database-free health check is still available without MySQL by running the default `local` profile:

```zsh
cd /Users/koushalbalimane/Desktop/CivicConnect/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run
curl http://localhost:8080/api/health
```
