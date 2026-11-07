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
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.common.security.ActorResolver;
import vn.edu.ptit.smarthms.hr.api.HrRequests.CreateRoleProfileRequest;
import vn.edu.ptit.smarthms.hr.api.HrResponses.RoleProfileResponse;
import vn.edu.ptit.smarthms.hr.service.RoleProfileService;

/** Role profiles (FR-HR-01, FR-HR-04). */
@RestController
@RequestMapping("/api/v1/hr/role-profiles")
public class RoleProfileController {

    private final RoleProfileService roleProfiles;

    public RoleProfileController(RoleProfileService roleProfiles) {
        this.roleProfiles = roleProfiles;
    }

    private Actor actor(Authentication authentication) {
        return ActorResolver.from(authentication);
    }

    @PostMapping
    @PreAuthorize("hasRole('HR_SPECIALIST')")
    public ResponseEntity<RoleProfileResponse> createRoleProfile(Authentication authentication,
            @Valid @RequestBody CreateRoleProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roleProfiles.createRoleProfile(actor(authentication), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'HR_APPROVER')")
    public List<RoleProfileResponse> listRoleProfiles(Authentication authentication) {
        return roleProfiles.listRoleProfiles(actor(authentication));
    }

    @GetMapping("/{roleProfileId}")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'HR_APPROVER')")
    public RoleProfileResponse getRoleProfile(Authentication authentication,
            @PathVariable UUID roleProfileId) {
        return roleProfiles.getRoleProfile(actor(authentication), roleProfileId);
    }
}
