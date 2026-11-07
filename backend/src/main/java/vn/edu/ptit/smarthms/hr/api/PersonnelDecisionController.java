package vn.edu.ptit.smarthms.hr.api;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.common.security.ActorResolver;
import vn.edu.ptit.smarthms.hr.api.HrRequests.CreateDecisionRequest;
import vn.edu.ptit.smarthms.hr.api.HrResponses.DecisionResponse;
import vn.edu.ptit.smarthms.hr.service.PersonnelDecisionService;

/** Personnel decisions (FR-HR-03, FR-HR-04). */
@RestController
@RequestMapping("/api/v1/hr")
public class PersonnelDecisionController {

    private final PersonnelDecisionService decisions;

    public PersonnelDecisionController(PersonnelDecisionService decisions) {
        this.decisions = decisions;
    }

    private Actor actor(Authentication authentication) {
        return ActorResolver.from(authentication);
    }

    @PostMapping("/employees/{employeeId}/decisions")
    @PreAuthorize("hasRole('HR_SPECIALIST')")
    public ResponseEntity<DecisionResponse> createDecision(Authentication authentication,
            @PathVariable UUID employeeId, @Valid @RequestBody CreateDecisionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(decisions.createDecision(actor(authentication), employeeId, request));
    }

    @GetMapping("/decisions")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'HR_APPROVER')")
    public List<DecisionResponse> listDecisions(Authentication authentication,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) String status) {
        return decisions.listDecisions(actor(authentication), employeeId, status);
    }

    @GetMapping("/decisions/{decisionId}")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'HR_APPROVER')")
    public DecisionResponse getDecision(Authentication authentication,
            @PathVariable UUID decisionId) {
        return decisions.getDecision(actor(authentication), decisionId);
    }

    @PostMapping("/decisions/{decisionId}/approve")
    @PreAuthorize("hasRole('HR_APPROVER')")
    public DecisionResponse approveDecision(Authentication authentication,
            @PathVariable UUID decisionId) {
        return decisions.approveDecision(actor(authentication), decisionId);
    }

    @PostMapping("/decisions/{decisionId}/revoke")
    @PreAuthorize("hasRole('HR_APPROVER')")
    public DecisionResponse revokeDecision(Authentication authentication,
            @PathVariable UUID decisionId) {
        return decisions.revokeDecision(actor(authentication), decisionId);
    }

    @PostMapping("/decisions/apply-due")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'HR_APPROVER')")
    public List<DecisionResponse> applyDueDecisions(Authentication authentication) {
        return decisions.applyDueDecisions(actor(authentication));
    }
}
