# be-interview-prep

Spring Boot 3.5 / Java 17 / Maven backend. One PR per question.

## Run

`JWT_SECRET` is required and must be at least 32 characters. `ADMIN_EMAIL` and `ADMIN_PASSWORD` are optional and seed an ADMIN user at startup.

    export JWT_SECRET=replace-with-a-secret-of-32-or-more-characters
    export ADMIN_EMAIL=admin@example.com
    export ADMIN_PASSWORD=change-me-admin
    ./mvnw spring-boot:run

On Windows PowerShell use `$env:JWT_SECRET = "..."` instead of `export`.

The app listens on port 8080 (override with `PORT`) and uses an in-memory H2 database.

- Health: http://localhost:8080/actuator/health
- Interactive API docs (Swagger UI): http://localhost:8080/swagger-ui.html. Log in via `POST /api/auth/login`, click Authorize and paste the token.
- A Bruno collection covering every endpoint is in `bruno/`.

## Test

    ./mvnw test

The test profile supplies its own JWT secret, so no environment variables are needed.

## Pull requests

| Question | PR |
|---|---|
| Q1 Task Manager API | [#9](https://github.com/bonyalex/be-interview-prep/pull/9) |
| Q2 URL Shortener | [#10](https://github.com/bonyalex/be-interview-prep/pull/10) |
| Q3 Authentication and roles | [#11](https://github.com/bonyalex/be-interview-prep/pull/11) |
| Q4 Product Catalog | [#12](https://github.com/bonyalex/be-interview-prep/pull/12) |
| Q5 Order Service | [#13](https://github.com/bonyalex/be-interview-prep/pull/13) |
| Optional Q1: interactive API documentation | [#16](https://github.com/bonyalex/be-interview-prep/pull/16) |
| Project scaffold | [#8](https://github.com/bonyalex/be-interview-prep/pull/8) |
| Video (YouTube, Unlisted) | [Watch](https://youtu.be/jhK7LUFWqzw) |
