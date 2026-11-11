package vn.edu.ptit.smarthms.system.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.common.audit.AuditService;
import vn.edu.ptit.smarthms.common.error.AppException;
import vn.edu.ptit.smarthms.common.error.ErrorCode;
import vn.edu.ptit.smarthms.common.security.TokenService;
import vn.edu.ptit.smarthms.common.util.SecurityUtils;
import vn.edu.ptit.smarthms.system.domain.Role;
import vn.edu.ptit.smarthms.system.domain.RoleAssignment;
import vn.edu.ptit.smarthms.system.domain.Session;
import vn.edu.ptit.smarthms.system.domain.UserAccount;
import vn.edu.ptit.smarthms.system.repository.RoleAssignmentRepository;
import vn.edu.ptit.smarthms.system.repository.RoleRepository;
import vn.edu.ptit.smarthms.system.repository.SessionRepository;
import vn.edu.ptit.smarthms.system.repository.UserAccountRepository;

@Service
public class SystemTestScenarioService {

    public static final UUID EMR_TEST_001_ID = UUID.fromString("e0000000-0000-0000-0000-000000000101");
    public static final UUID EMR_TEST_002_ID = UUID.fromString("e0000000-0000-0000-0000-000000000102");
    public static final UUID ORD_CLS_001_ID = UUID.fromString("e0000000-0000-0000-0000-000000000201");
    public static final UUID EMP_DEMO_001_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    public static final UUID AUD_TEST_001_ID = UUID.fromString("f0000000-0000-0000-0000-000000000001");
    public static final UUID SESSION_EXPIRED_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    public static final UUID SESSION_ACTIVE_ID = UUID.fromString("c0000000-0000-0000-0000-000000000002");

    private static final Set<String> APPROVED_SCOPES = Set.of("OWN", "DEPT", "ALL");

    private final JdbcTemplate jdbcTemplate;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final SessionRepository sessionRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public SystemTestScenarioService(JdbcTemplate jdbcTemplate,
                                     UserAccountRepository userAccountRepository,
                                     RoleRepository roleRepository,
                                     RoleAssignmentRepository roleAssignmentRepository,
                                     SessionRepository sessionRepository,
                                     TokenService tokenService,
                                     PasswordEncoder passwordEncoder,
                                     AuditService auditService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.sessionRepository = sessionRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public UUID resolveRecordId(String recordRef) {
        if ("EMR-TEST-001".equalsIgnoreCase(recordRef)) {
            return EMR_TEST_001_ID;
        }
        if ("EMR-TEST-002".equalsIgnoreCase(recordRef)) {
            return EMR_TEST_002_ID;
        }
        try {
            return UUID.fromString(recordRef);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Unknown EMR record reference: " + recordRef);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> readEmrRecord(String recordRef) {
        UUID recordId = resolveRecordId(recordRef);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT record_id, encounter_id, record_status, current_version, row_version, locked_at, locked_by " +
                        "FROM emr.medical_record WHERE record_id = ?",
                recordId
        );
        if (rows.isEmpty()) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Medical record not found: " + recordRef);
        }
        Map<String, Object> row = rows.get(0);
        auditService.success(
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUsername(),
                "EMR_RECORD",
                recordRef,
                "READ",
                "V" + row.get("current_version"),
                Map.of("rowVersion", row.get("row_version"), "recordStatus", row.get("record_status"))
        );
        return row;
    }

    @Transactional
    public Map<String, Object> lockEmrRecord(String recordRef) {
        UUID recordId = resolveRecordId(recordRef);
        UUID actorUserId = SecurityUtils.getCurrentUserId();
        String actorUsername = SecurityUtils.getCurrentUsername();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT record_status, current_version, row_version FROM emr.medical_record WHERE record_id = ?",
                recordId
        );
        if (rows.isEmpty()) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Medical record not found: " + recordRef);
        }
        String currentStatus = String.valueOf(rows.get(0).get("record_status"));
        if ("LOCKED".equals(currentStatus) || "ARCHIVED".equals(currentStatus)) {
            throw new AppException(ErrorCode.CONFLICT, "Medical record is already locked: " + recordRef);
        }

        int updated = jdbcTemplate.update(
                "UPDATE emr.medical_record SET record_status = 'LOCKED', locked_at = CURRENT_TIMESTAMP, " +
                        "locked_by = ?, row_version = row_version + 1, updated_at = CURRENT_TIMESTAMP, updated_by = ? " +
                        "WHERE record_id = ?",
                actorUserId,
                actorUsername != null ? actorUsername : "system",
                recordId
        );
        if (updated == 0) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Medical record not found: " + recordRef);
        }

        Map<String, Object> after = jdbcTemplate.queryForMap(
                "SELECT record_id, record_status, current_version, row_version, locked_at, locked_by " +
                        "FROM emr.medical_record WHERE record_id = ?",
                recordId
        );

        String changeRef = "LOCK-" + recordRef + "-RV" + after.get("row_version");
        auditService.success(
                actorUserId,
                actorUsername,
                "EMR_RECORD",
                recordRef,
                "LOCK_RECORD",
                changeRef,
                Map.of(
                        "recordStatus", after.get("record_status"),
                        "rowVersion", after.get("row_version"),
                        "currentVersion", after.get("current_version")
                )
        );
        return after;
    }

    @Transactional
    public Map<String, Object> updateEmployeeStatusWithDecision(String employeeRef,
                                                                String newStatus,
                                                                String decisionRef,
                                                                String reason) {
        if (decisionRef == null || decisionRef.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "decisionRef is required for HR status update");
        }
        UUID employeeId = "EMP-DEMO-001".equalsIgnoreCase(employeeRef)
                ? EMP_DEMO_001_ID
                : UUID.fromString(employeeRef);

        String actor = SecurityUtils.getCurrentUsername() != null ? SecurityUtils.getCurrentUsername() : "system";
        int updated = jdbcTemplate.update(
                "UPDATE hr.employee SET status = ?, status_effective_at = CURRENT_TIMESTAMP, " +
                        "row_version = row_version + 1, updated_at = CURRENT_TIMESTAMP, updated_by = ? " +
                        "WHERE employee_id = ?",
                newStatus,
                actor,
                employeeId
        );
        if (updated == 0) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Employee not found: " + employeeRef);
        }

        Map<String, Object> empAfter = jdbcTemplate.queryForMap(
                "SELECT employee_id, employee_code, status, row_version FROM hr.employee WHERE employee_id = ?",
                employeeId
        );

        auditService.success(
                SecurityUtils.getCurrentUserId(),
                actor,
                "HR_EMPLOYEE",
                String.valueOf(empAfter.get("employee_code")),
                "UPDATE_STATUS",
                decisionRef,
                Map.of(
                        "newStatus", newStatus,
                        "rowVersion", empAfter.get("row_version"),
                        "decisionRef", decisionRef
                )
        );
        return empAfter;
    }

    @Transactional
    public Map<String, Object> assignRoleWithDecision(String username,
                                                      String roleCode,
                                                      String approvalRef,
                                                      String scopeCode,
                                                      Integer validMinutes) {
        UUID actorUserId = SecurityUtils.getCurrentUserId();
        String actorUsername = SecurityUtils.getCurrentUsername() != null ? SecurityUtils.getCurrentUsername() : "system";

        if (approvalRef == null || approvalRef.isBlank() || !approvalRef.startsWith("DEC-RBAC-")) {
            auditService.denied(actorUserId, actorUsername, "SYS_ROLE_ASSIGNMENT", username, "ASSIGN_ROLE",
                    Map.of("reason", "MISSING_OR_INVALID_APPROVAL_REF"));
            throw new AppException(ErrorCode.ACCESS_DENIED, "Role assignment requires approved DEC-RBAC-* reference");
        }

        if (scopeCode != null && !APPROVED_SCOPES.contains(scopeCode)) {
            auditService.denied(actorUserId, actorUsername, "SYS_ROLE_ASSIGNMENT", username, "ASSIGN_ROLE",
                    Map.of("reason", "UNAPPROVED_SCOPE_OPEN_01", "requestedScope", scopeCode));
            throw new AppException(ErrorCode.ACCESS_DENIED, "Unapproved scope requested (OPEN-01): " + scopeCode);
        }

        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found: " + username));
        Role role = roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found: " + roleCode));

        Instant now = Instant.now();
        Instant endAt = (validMinutes != null && validMinutes > 0)
                ? now.plus(validMinutes, ChronoUnit.MINUTES)
                : now.plus(24, ChronoUnit.HOURS);

        RoleAssignment ra = new RoleAssignment();
        ra.setAssignmentId(UUID.randomUUID());
        ra.setUser(user);
        ra.setRole(role);
        ra.setStartAt(now.minusSeconds(1));
        ra.setEndAt(endAt);
        ra.setApprovalRef(approvalRef);
        ra.setApprovedBy(actorUsername);
        roleAssignmentRepository.save(ra);

        auditService.success(
                actorUserId,
                actorUsername,
                "SYS_ROLE_ASSIGNMENT",
                ra.getAssignmentId().toString(),
                "ASSIGN_ROLE",
                approvalRef,
                Map.of("targetUser", username, "roleCode", roleCode, "scope", scopeCode != null ? scopeCode : "DEFAULT")
        );

        return Map.of(
                "assignmentId", ra.getAssignmentId(),
                "username", username,
                "roleCode", roleCode,
                "approvalRef", approvalRef,
                "startAt", ra.getStartAt(),
                "endAt", ra.getEndAt()
        );
    }

    @Transactional
    public Map<String, Object> revokeRoleAssignment(String username, String roleCode, String approvalRef) {
        UUID actorUserId = SecurityUtils.getCurrentUserId();
        String actorUsername = SecurityUtils.getCurrentUsername() != null ? SecurityUtils.getCurrentUsername() : "system";

        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found: " + username));

        Instant now = Instant.now();
        List<RoleAssignment> activeList = roleAssignmentRepository.findActiveByUserId(user.getUserId(), now);
        int revokedCount = 0;
        for (RoleAssignment ra : activeList) {
            if (roleCode == null || roleCode.isBlank() || ra.getRole().getRoleCode().equals(roleCode)) {
                ra.setEndAt(ra.getStartAt().plusMillis(1));
                roleAssignmentRepository.save(ra);
                revokedCount++;
            }
        }

        auditService.success(
                actorUserId,
                actorUsername,
                "SYS_ROLE_ASSIGNMENT",
                username,
                "REVOKE_ROLE",
                approvalRef != null ? approvalRef : "DEC-RBAC-REVOKE",
                Map.of("targetUser", username, "revokedCount", revokedCount)
        );

        return Map.of(
                "username", username,
                "revokedCount", revokedCount,
                "approvalRef", approvalRef != null ? approvalRef : "DEC-RBAC-REVOKE"
        );
    }

    @Transactional(noRollbackFor = AppException.class)
    public Map<String, Object> processMockIntegrationEvent(String eventType,
                                                           String eventKey,
                                                           String sourceSystem,
                                                           String sourceVersion,
                                                           boolean simulateFailure) {
        UUID actorUserId = SecurityUtils.getCurrentUserId();
        String actor = SecurityUtils.getCurrentUsername() != null ? SecurityUtils.getCurrentUsername() : "system";

        if ("CLS_RESULT".equalsIgnoreCase(eventType)) {
            String sys = (sourceSystem != null && !sourceSystem.isBlank()) ? sourceSystem : "LIS-MOCK";
            String ver = (sourceVersion != null && !sourceVersion.isBlank()) ? sourceVersion : "v1";

            List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                    "SELECT result_id, quality FROM emr.clinical_result " +
                            "WHERE source_system = ? AND source_result_id = ? AND source_version = ?",
                    sys, eventKey, ver
            );

            if (!existing.isEmpty()) {
                Integer totalCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM emr.clinical_result WHERE source_result_id = ?",
                        Integer.class, eventKey
                );
                auditService.success(actorUserId, actor, "EMR_CLINICAL_RESULT", eventKey, "IDEMPOTENT_DUPLICATE_IGNORED",
                        ver, Map.of("duplicate", true, "recordCount", totalCount != null ? totalCount : 1));
                return Map.of(
                        "eventKey", eventKey,
                        "eventType", "CLS_RESULT",
                        "duplicate", true,
                        "deliveryState", "SENT",
                        "recordCount", totalCount != null ? totalCount : 1
                );
            }

            if (simulateFailure) {
                auditService.failure(actorUserId, actor, "EMR_CLINICAL_RESULT", eventKey, "CLS_INGEST_TIMEOUT",
                        Map.of("simulatedTimeout", true, "sourceSystem", sys));
                throw new AppException(ErrorCode.INTEGRATION_DELIVERY_FAILED,
                        "Simulated adapter timeout for CLS event: " + eventKey);
            }

            UUID resultId = UUID.randomUUID();
            jdbcTemplate.update(
                    "INSERT INTO emr.clinical_result (result_id, order_id, source_system, source_result_id, source_version, " +
                            "received_at, result_at, result_payload, abnormal_flag, quality, created_by, updated_by) " +
                            "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{\"value\":\"NORMAL\"}'::jsonb, false, 'VALID', ?, ?)",
                    resultId, ORD_CLS_001_ID, sys, eventKey, ver, actor, actor
            );

            Integer totalCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM emr.clinical_result WHERE source_result_id = ?",
                    Integer.class, eventKey
            );
            auditService.success(actorUserId, actor, "EMR_CLINICAL_RESULT", eventKey, "CLS_INGEST",
                    ver, Map.of("duplicate", false, "recordCount", totalCount != null ? totalCount : 1));

            return Map.of(
                    "eventKey", eventKey,
                    "eventType", "CLS_RESULT",
                    "duplicate", false,
                    "deliveryState", "SENT",
                    "recordCount", totalCount != null ? totalCount : 1
            );
        }

        // Default: HR_OUTBOX (hr.integration_outbox)
        List<Map<String, Object>> existingOutbox = jdbcTemplate.queryForList(
                "SELECT event_id, delivery_state, attempts FROM hr.integration_outbox WHERE event_key = ?",
                eventKey
        );

        if (existingOutbox.isEmpty()) {
            UUID eventId = UUID.randomUUID();
            String initialState = simulateFailure ? "FAILED" : "SENT";
            jdbcTemplate.update(
                    "INSERT INTO hr.integration_outbox (event_id, employee_id, event_key, event_type, payload, " +
                            "delivery_state, attempts, last_attempt_at, created_by, updated_by) " +
                            "VALUES (?, ?, ?, 'HR_STATUS_CHANGED', '{\"synthetic\":true}'::jsonb, ?, 1, CURRENT_TIMESTAMP, ?, ?)",
                    eventId, EMP_DEMO_001_ID, eventKey, initialState, actor, actor
            );
            if (simulateFailure) {
                auditService.failure(actorUserId, actor, "HR_OUTBOX", eventKey, "OUTBOX_DELIVERY_TIMEOUT",
                        Map.of("deliveryState", "FAILED", "attempts", 1));
                throw new AppException(ErrorCode.INTEGRATION_DELIVERY_FAILED,
                        "Simulated timeout after mock received event: " + eventKey);
            }
            auditService.success(actorUserId, actor, "HR_OUTBOX", eventKey, "OUTBOX_DELIVERED",
                    eventKey, Map.of("deliveryState", "SENT", "attempts", 1, "duplicate", false));
            return Map.of(
                    "eventKey", eventKey,
                    "eventType", "HR_OUTBOX",
                    "duplicate", false,
                    "deliveryState", "SENT",
                    "attempts", 1,
                    "recordCount", 1
            );
        }

        Map<String, Object> row = existingOutbox.get(0);
        String currentState = String.valueOf(row.get("delivery_state"));
        int currentAttempts = ((Number) row.get("attempts")).intValue();

        if ("SENT".equals(currentState)) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM hr.integration_outbox WHERE event_key = ?",
                    Integer.class, eventKey
            );
            auditService.success(actorUserId, actor, "HR_OUTBOX", eventKey, "OUTBOX_IDEMPOTENT_SKIP",
                    eventKey, Map.of("deliveryState", "SENT", "attempts", currentAttempts, "duplicate", true));
            return Map.of(
                    "eventKey", eventKey,
                    "eventType", "HR_OUTBOX",
                    "duplicate", true,
                    "deliveryState", "SENT",
                    "attempts", currentAttempts,
                    "recordCount", count != null ? count : 1
            );
        }

        int nextAttempts = currentAttempts + 1;
        String nextState = simulateFailure ? "FAILED" : "SENT";
        jdbcTemplate.update(
                "UPDATE hr.integration_outbox SET delivery_state = ?, attempts = ?, last_attempt_at = CURRENT_TIMESTAMP, " +
                        "updated_at = CURRENT_TIMESTAMP, updated_by = ? WHERE event_key = ?",
                nextState, nextAttempts, actor, eventKey
        );

        if (simulateFailure) {
            auditService.failure(actorUserId, actor, "HR_OUTBOX", eventKey, "OUTBOX_DELIVERY_TIMEOUT",
                    Map.of("deliveryState", "FAILED", "attempts", nextAttempts));
            throw new AppException(ErrorCode.INTEGRATION_DELIVERY_FAILED,
                    "Simulated timeout on retry for event: " + eventKey);
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hr.integration_outbox WHERE event_key = ?",
                Integer.class, eventKey
        );
        auditService.success(actorUserId, actor, "HR_OUTBOX", eventKey, "OUTBOX_RETRY_RECONCILED",
                eventKey, Map.of("deliveryState", "SENT", "attempts", nextAttempts, "duplicate", false));
        return Map.of(
                "eventKey", eventKey,
                "eventType", "HR_OUTBOX",
                "duplicate", false,
                "deliveryState", "SENT",
                "attempts", nextAttempts,
                "recordCount", count != null ? count : 1
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> takeSnapshot() {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("timestamp", Instant.now().toString());
        snap.put("sessionCount", sessionRepository.count());
        snap.put("auditCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys.audit_event", Long.class));
        snap.put("activeDocRoleCount", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys.role_assignment ra JOIN sys.user_account u ON ra.user_id = u.user_id " +
                        "WHERE u.username = 'DOC-TEST-001' AND ra.start_at <= CURRENT_TIMESTAMP " +
                        "AND (ra.end_at IS NULL OR ra.end_at > CURRENT_TIMESTAMP)",
                Long.class
        ));
        snap.put("emrTest001", jdbcTemplate.queryForMap(
                "SELECT record_status, current_version, row_version FROM emr.medical_record WHERE record_id = ?",
                EMR_TEST_001_ID
        ));
        snap.put("emrTest002", jdbcTemplate.queryForMap(
                "SELECT record_status, current_version, row_version FROM emr.medical_record WHERE record_id = ?",
                EMR_TEST_002_ID
        ));
        snap.put("empDemo001", jdbcTemplate.queryForMap(
                "SELECT status, row_version FROM hr.employee WHERE employee_id = ?",
                EMP_DEMO_001_ID
        ));
        snap.put("audTest001", jdbcTemplate.queryForMap(
                "SELECT audit_id, actor_ref, action, outcome FROM sys.audit_event WHERE audit_id = ?",
                AUD_TEST_001_ID
        ));
        snap.put("outboxCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hr.integration_outbox", Long.class));
        snap.put("clsResultCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM emr.clinical_result", Long.class));
        return snap;
    }

    @Transactional
    public Map<String, Object> resetFixtures() {
        syncAccountPasswords();

        // 1. Restore DOC-TEST-001 active ROLE_DOCTOR assignment
        jdbcTemplate.update(
                "UPDATE sys.role_assignment SET start_at = CURRENT_TIMESTAMP - INTERVAL '1 day', end_at = NULL " +
                        "WHERE assignment_id = 'b0000000-0000-0000-0000-000000000011'"
        );
        // Remove temporary non-bootstrap role assignments created during tests
        jdbcTemplate.update(
                "DELETE FROM sys.role_assignment WHERE assignment_id NOT IN (" +
                        "'b0000000-0000-0000-0000-000000000001'," +
                        "'b0000000-0000-0000-0000-000000000002'," +
                        "'b0000000-0000-0000-0000-000000000011'," +
                        "'b0000000-0000-0000-0000-000000000012'," +
                        "'b0000000-0000-0000-0000-000000000013'," +
                        "'b0000000-0000-0000-0000-000000000014'" +
                        ")"
        );

        // 2. Restore SESSION-EXPIRED-001 and SESSION-ACTIVE-001
        UserAccount docUser = userAccountRepository.findByUsername("DOC-TEST-001").orElse(null);
        if (docUser != null) {
            upsertNamedSession(SESSION_EXPIRED_ID, docUser, "SESSION-EXPIRED-001",
                    Instant.now().minus(10, ChronoUnit.HOURS),
                    Instant.now().minus(2, ChronoUnit.HOURS));
            upsertNamedSession(SESSION_ACTIVE_ID, docUser, "SESSION-ACTIVE-001",
                    Instant.now().minus(1, ChronoUnit.HOURS),
                    Instant.now().plus(7, ChronoUnit.HOURS));
        }

        // 3. Reset EMR-TEST-001 and EMR-TEST-002 to DRAFT (row_version = 1)
        jdbcTemplate.execute("ALTER TABLE emr.medical_record DISABLE TRIGGER trg_record_locked");
        try {
            jdbcTemplate.update(
                    "UPDATE emr.medical_record SET record_status = 'DRAFT', locked_at = NULL, locked_by = NULL, " +
                            "current_version = 1, row_version = 1 WHERE record_id IN (?, ?)",
                    EMR_TEST_001_ID, EMR_TEST_002_ID
            );
        } finally {
            jdbcTemplate.execute("ALTER TABLE emr.medical_record ENABLE TRIGGER trg_record_locked");
        }

        // 4. Reset EMP-DEMO-001 status and row_version
        jdbcTemplate.update(
                "UPDATE hr.employee SET status = 'ACTIVE', row_version = 1 WHERE employee_id = ?",
                EMP_DEMO_001_ID
        );

        // 5. Clean up test integration outbox & CLS result fixtures
        jdbcTemplate.update("DELETE FROM hr.integration_outbox WHERE event_key LIKE 'EV-%'");
        jdbcTemplate.update("DELETE FROM emr.clinical_result WHERE source_result_id LIKE 'EV-%'");

        return takeSnapshot();
    }

    @Transactional
    public void syncAccountPasswords() {
        Map<String, String> credentials = Map.of(
                "admin", "Admin@2026!",
                "doctor1", "Doctor@2026!",
                "DOC-TEST-001", "Doctor@2026!",
                "IT-ADMIN-TEST-001", "Admin@2026!",
                "NURSE-TEST-001", "Nurse@2026!",
                "HR-TEST-001", "Hr@2026!",
                "HR-UNAPPROVED-DOC", "Unapproved@2026!"
        );
        credentials.forEach((username, rawPass) -> {
            userAccountRepository.findByUsername(username).ifPresent(acc -> {
                if (!passwordEncoder.matches(rawPass, acc.getPasswordHash())) {
                    acc.setPasswordHash(passwordEncoder.encode(rawPass));
                    userAccountRepository.save(acc);
                }
            });
        });
    }

    private void upsertNamedSession(UUID sessionId, UserAccount user, String rawToken, Instant issuedAt, Instant expiresAt) {
        String digest = tokenService.hashToken(rawToken);
        jdbcTemplate.update(
                "INSERT INTO sys.session (session_id, user_id, token_digest, issued_at, expires_at, revoked_at, issued_epoch, created_by, updated_by) " +
                        "VALUES (?, ?, ?, ?, ?, NULL, ?, 'system', 'system') " +
                        "ON CONFLICT (session_id) DO UPDATE SET token_digest = EXCLUDED.token_digest, " +
                        "issued_at = EXCLUDED.issued_at, expires_at = EXCLUDED.expires_at, revoked_at = NULL",
                sessionId,
                user.getUserId(),
                digest,
                Timestamp.from(issuedAt),
                Timestamp.from(expiresAt),
                user.getRevocationEpoch()
        );
    }
}
