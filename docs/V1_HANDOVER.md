# V.1 implementation handover

## Structure and ownership

- `backend/src/common`: cross-cutting extension points only; no module business
  rules. SYS/common owns accounts, roles, permissions, sessions, and audit.
- `backend/src/modules/emr`: owns encounter/clinical-record behavior only.
- `backend/src/modules/hr`: owns employment and professional evidence only.
- `frontend`: reserved until a framework is accepted.

EMR and HR communicate through approved typed, in-process ports. They must not
reach into one another's persistence, introduce circular dependencies, or
become internal HTTP/microservices. Patient, appointment, allergy, medication,
CLS, and other out-of-scope data are fixtures/adapters only when approved and
needed by EMR.

## Work handoff

| Work package | Start here | Preconditions |
| --- | --- | --- |
| V.2 common/security | `backend/src/common` | Accept backend stack; resolve IV.6 policy. Implement auth, RBAC, audit, error handling with default deny. |
| V.3 EMR | `backend/src/modules/emr` | Accept EMR API/schema decisions and approved fixture/port contracts. |
| V.4 HR | `backend/src/modules/hr` | Accept HR API/schema decisions; publish practitioner facts through an approved port. |
| V.5 integration | module boundaries above | Use typed in-process ports; freeze contract ownership before wiring. |
| V.6 demo/test | approved database/test setup | Add synthetic demo data/account seed and end-to-end tests only after migration strategy exists. |

## Implementation rules

- Add the chosen manifest and its lockfile together after approval.
- Put non-secret examples in an approved environment example; retain `.env`
  ignore protection and validate required variables at startup.
- Convert IV.2 DDL into executable migrations only after DBMS/tooling approval;
  keep SCP-IV4-001 out until unblocked.
- Record actual commands, exit codes, and environment assumptions in
  `V1_VERIFICATION.md`; never mark a static review as runtime PASS.


---

## V.2 Completion Notes

**Completed:** 2026-11-09

### What was implemented

| Task | Status | Notes |
|------|--------|-------|
| pom.xml – Lombok dependency + compiler plugin | ✅ Done | Lombok added with annotation processor config |
| V3__auth_seed.sql | ✅ Done | Roles, permissions, role_permission, admin user, role assignment |
| JPA Entities (sys package) | ✅ Done | AuditableEntity, UserAccount, Role, Permission, RoleAssignment, Session, AuditEvent |
| JPA Repositories | ✅ Done | UserAccountRepository, SessionRepository, AuditEventRepository, RoleAssignmentRepository |
| TokenService (opaque token / SHA-256) | ✅ Done | UUID v4 tokens, SHA-256 digest stored in DB, 8h TTL |
| SmartHmsUserDetailsService | ✅ Done | Loads by username and by token; builds role + permission authorities |
| BearerTokenFilter | ✅ Done | OncePerRequestFilter; validates opaque token before Spring Security filters |
| SecurityConfiguration | ✅ Done | Stateless; CSRF off; permit login+health; BCrypt strength 12 |
| Auth REST API (/api/v1/auth) | ✅ Done | POST /login, POST /logout, GET /me |
| AuditService + @Async | ✅ Done | recordEvent, recordEventAsync, success/denied/failure helpers |
| AuditContext + RequestAuditFilter | ✅ Done | Per-request correlationId in MDC + X-Correlation-Id header |
| ErrorCode enum + AppException + ErrorResponse | ✅ Done | 9 typed error codes with HTTP status |
| ApiExceptionHandler | ✅ Done | Handles AppException, validation, AccessDenied, AuthenticationException, Exception |
| CorrelationIdHolder, SecurityUtils | ✅ Done | Thread-local correlation; SecurityContext helpers |
| @RequiresPermission annotation | ✅ Done | Custom method-level annotation |
| PermissionCheckAspect | ✅ Done | @Around AOP; checks resource:action:scope; throws ACCESS_DENIED |
| JpaAuditingConfiguration | ✅ Done | @EnableJpaAuditing; AuditorAware reads username from SecurityContext |
| AsyncConfiguration | ✅ Done | @EnableAsync configuration class |
| application.yml updates | ✅ Done | app.security.*, spring.task.execution.pool |
| SessionCleanupService | ✅ Done | @Scheduled(fixedDelayString="PT1H"); deletes expired sessions |
| @EnableScheduling | ✅ Done | Added to SmartHmsApplication |
| Unit tests | ✅ Done | TokenServiceTest, ApiExceptionHandlerTest, AuthControllerTest |

### Architecture decisions

- **Opaque tokens**: Raw UUID v4 is returned to the client; SHA-256 hex digest is stored in `sys.session`. No JWT dependency added.
- **Permission authority format**: `RESOURCE:ACTION:SCOPE` strings as Spring GrantedAuthority (e.g., `EMR_ENCOUNTER:READ:OWN`).
- **Role authority format**: `ROLE_xxx` strings (e.g., `ROLE_ADMIN`) matching `sys.role.role_code`.
- **Filter order**: `RequestAuditFilter` → `BearerTokenFilter` → Spring Security filters.
- **Audit writes**: Async by default via `@Async` to avoid blocking request threads.

### Default admin credentials (dev only)

- Username: `admin`
- Password: `Admin@2026!`
- **Change immediately in production.**

### Next steps for V.3 (EMR)

- Implement encounter CRUD with `@RequiresPermission(resource="EMR_ENCOUNTER", action="READ", scope="OWN")`
- Use `SecurityUtils.getCurrentUserId()` to scope data access
- Add audit calls via `AuditService.success(...)` on mutations
