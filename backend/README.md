# Backend

Java 21 / Spring Boot 4.1.2 / Maven foundation. Run `mvnw.cmd clean verify` on
Windows or `./mvnw clean verify` on Unix; the wrapper downloads Maven 3.9.11
locally if needed. Flyway owns schema changes in `src/main/resources/db/migration`;
JPA is validate-only. Configure `DATABASE_URL`, `DATABASE_USERNAME`, and
`DATABASE_PASSWORD` through environment variables.

Packages: `common` for infrastructure, `system.api` for health only, `emr` and
`hr` for future owned modules. V.1 permits only `/actuator/health` and
`/api/v1/system/health`; all other requests are denied.
