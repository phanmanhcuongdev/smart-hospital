package vn.edu.ptit.smarthms.system.api;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.common.annotation.RequiresPermission;
import vn.edu.ptit.smarthms.system.service.SystemTestScenarioService;

@RestController
@RequestMapping("/api/v1")
public class SystemTestScenarioController {

    private final SystemTestScenarioService scenarioService;

    public SystemTestScenarioController(SystemTestScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @GetMapping("/emr/records/{recordRef}")
    @RequiresPermission(resource = "EMR_RECORD", action = "READ", scope = "ANY")
    public ResponseEntity<Map<String, Object>> getEmrRecord(@PathVariable String recordRef) {
        return ResponseEntity.ok(scenarioService.readEmrRecord(recordRef));
    }

    @PostMapping("/emr/records/{recordRef}/lock")
    @RequiresPermission(resource = "EMR_RECORD", action = "LOCK", scope = "OWN")
    public ResponseEntity<Map<String, Object>> lockEmrRecord(@PathVariable String recordRef) {
        return ResponseEntity.ok(scenarioService.lockEmrRecord(recordRef));
    }

    @PostMapping("/hr/employees/{employeeRef}/status")
    @RequiresPermission(resource = "HR_EMPLOYEE", action = "WRITE", scope = "ALL")
    public ResponseEntity<Map<String, Object>> updateEmployeeStatus(
            @PathVariable String employeeRef,
            @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "ACTIVE");
        String decisionRef = body.get("decisionRef");
        String reason = body.getOrDefault("reason", "HR status transition");
        return ResponseEntity.ok(scenarioService.updateEmployeeStatusWithDecision(employeeRef, status, decisionRef, reason));
    }

    @PostMapping("/sys/role-assignments")
    @RequiresPermission(resource = "SYS_ROLE", action = "ASSIGN", scope = "ALL")
    public ResponseEntity<Map<String, Object>> assignRole(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String roleCode = (String) body.get("roleCode");
        String approvalRef = (String) body.get("approvalRef");
        String scopeCode = (String) body.get("scopeCode");
        Integer validMinutes = body.get("validMinutes") instanceof Number n ? n.intValue() : 60;
        return ResponseEntity.ok(scenarioService.assignRoleWithDecision(username, roleCode, approvalRef, scopeCode, validMinutes));
    }

    @PostMapping("/sys/role-assignments/revoke")
    @RequiresPermission(resource = "SYS_ROLE", action = "ASSIGN", scope = "ALL")
    public ResponseEntity<Map<String, Object>> revokeRole(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String roleCode = body.get("roleCode");
        String approvalRef = body.get("approvalRef");
        return ResponseEntity.ok(scenarioService.revokeRoleAssignment(username, roleCode, approvalRef));
    }

    @PostMapping("/integration/mock-delivery")
    public ResponseEntity<Map<String, Object>> processMockDelivery(@RequestBody Map<String, Object> body) {
        String eventType = (String) body.getOrDefault("eventType", "HR_OUTBOX");
        String eventKey = (String) body.getOrDefault("eventKey", "EV-HR-001");
        String sourceSystem = (String) body.getOrDefault("sourceSystem", "LIS-MOCK");
        String sourceVersion = (String) body.getOrDefault("sourceVersion", "v1");
        boolean simulateFailure = Boolean.TRUE.equals(body.get("simulateFailure"));
        return ResponseEntity.ok(scenarioService.processMockIntegrationEvent(
                eventType, eventKey, sourceSystem, sourceVersion, simulateFailure
        ));
    }

    @GetMapping("/sys/test-fixtures/snapshot")
    public ResponseEntity<Map<String, Object>> getSnapshot() {
        return ResponseEntity.ok(scenarioService.takeSnapshot());
    }

    @PostMapping("/sys/test-fixtures/reset")
    public ResponseEntity<Map<String, Object>> resetFixtures() {
        return ResponseEntity.ok(scenarioService.resetFixtures());
    }
}
