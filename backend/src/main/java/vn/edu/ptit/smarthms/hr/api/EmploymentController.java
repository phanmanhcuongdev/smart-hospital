package vn.edu.ptit.smarthms.hr.api;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.common.security.ActorResolver;
import vn.edu.ptit.smarthms.hr.api.HrRequests.CreateTransferRequest;
import vn.edu.ptit.smarthms.hr.api.HrResponses.AssignmentResponse;
import vn.edu.ptit.smarthms.hr.service.EmploymentService;

/** Transfers (FR-HR-04). */
@RestController
@RequestMapping("/api/v1/hr")
public class EmploymentController {

    private final EmploymentService employments;

    public EmploymentController(EmploymentService employments) {
        this.employments = employments;
    }

    private Actor actor(Authentication authentication) {
        return ActorResolver.from(authentication);
    }

    @PostMapping("/employees/{employeeId}/transfers")
    @PreAuthorize("hasRole('HR_SPECIALIST')")
    public ResponseEntity<AssignmentResponse> createTransfer(Authentication authentication,
            @PathVariable UUID employeeId, @Valid @RequestBody CreateTransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employments.createTransfer(actor(authentication), employeeId, request));
    }
}
