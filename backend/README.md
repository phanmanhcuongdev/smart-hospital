# Backend

Java 21 / Spring Boot 4.1.2 / Maven foundation. Run the system Maven with mvn clean verify from the backend directory. The Maven wrapper (mvnw) cannot download Maven 3.9.11 in this Codespace (archive.apache.org is unreachable); use the SDKMAN system Maven instead.

Flyway owns schema changes in src/main/resources/db/migration, and JPA is validate-only. Configure DATABASE_URL, DATABASE_USERNAME, and DATABASE_PASSWORD through environment variables. Flyway migrates before JPA validation: FlywayConfiguration defines a flywayInitializer bean that runs the migration during bean initialization, and a BeanFactoryPostProcessor makes the EntityManagerFactory depend on it. (Spring Boot 4.1.2 does not auto-migrate Flyway before JPA validation here, and the earlier CommandLineRunner design ran the migration too late on a fresh database.)

Packages: common for infrastructure, system.api for health only, emr for the Examination and Medical Record module (FR-EMR-01 to FR-EMR-04), hr for the Human Resource Management module (FR-HR-01 to FR-HR-04).

Security is HTTP Basic with demo users (demo password comes from SMARTHMS_DEMO_PASSWORD, default demo, demo-only): EMP-DEMO-001 is a doctor, demo-nurse, demo-cls, demo-records, demo-reception, demo-hr, demo-hr-approver. /actuator/health and /api/v1/system/health permit all requests; /api/v1/emr/ and /api/v1/hr/ require authentication; all other requests are denied.

Run locally: start PostgreSQL with docker compose up -d db (POSTGRES_PASSWORD must be set), build the jar with mvn package, then run the jar with DATABASE_URL, DATABASE_USERNAME and DATABASE_PASSWORD set. All data is synthetic demo data; no real hospital data is used.
