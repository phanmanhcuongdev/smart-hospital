package vn.edu.ptit.smarthms.hr.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.common.error.ConflictException;
import vn.edu.ptit.smarthms.common.error.NotFoundException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.hr.api.HrRequests.CreateRoleProfileRequest;
import vn.edu.ptit.smarthms.hr.api.HrRequests.UpdateRoleProfileRequest;
import vn.edu.ptit.smarthms.hr.api.HrResponses.RoleProfileResponse;
import vn.edu.ptit.smarthms.hr.domain.RoleProfile;
import vn.edu.ptit.smarthms.hr.domain.RoleProfileRepository;
import vn.edu.ptit.smarthms.hr.domain.EmploymentRepository;

/** Role profiles (FR-HR-01, FR-HR-04). A title is never a system role (BR-015). */
@Service
@Transactional
public class RoleProfileService {

    private final RoleProfileRepository roleProfiles;
  private final EmploymentRepository employments;

    public RoleProfileService(RoleProfileRepository roleProfiles, EmploymentRepository employments) {
    this.employments = employments;
        this.roleProfiles = roleProfiles;
    }

    public RoleProfileResponse createRoleProfile(Actor actor, CreateRoleProfileRequest request) {
        actor.requireRole("HR_SPECIALIST");
        if (roleProfiles.existsByTitleName(request.titleName())) {
            throw new ConflictException("Role profile already exists: " + request.titleName());
        }
        RoleProfile roleProfile = new RoleProfile(request.titleName());
        roleProfile.setProfessionalGroup(request.professionalGroup());
        roleProfile.setQualification(request.qualification());
        roleProfile.setDescription(request.description());
        roleProfile.setCreatedBy(actor.principalName());
        roleProfile.setUpdatedBy(actor.principalName());
        return RoleProfileResponse.from(roleProfiles.save(roleProfile));
    }

    public RoleProfileResponse updateRoleProfile(Actor actor, UUID roleProfileId, UpdateRoleProfileRequest request) {
    actor.requireRole("HR_SPECIALIST");
    RoleProfile profile = requireRoleProfile(roleProfileId);
    if (request.titleName() != null && !request.titleName().equals(profile.getTitleName())
        && roleProfiles.existsByTitleName(request.titleName())) {
      throw new ConflictException("A role profile with this title already exists.");
    }
    if (request.titleName() != null) profile.setTitleName(request.titleName());
    if (request.professionalGroup() != null) profile.setProfessionalGroup(request.professionalGroup());
    if (request.qualification() != null) profile.setQualification(request.qualification());
    if (request.description() != null) profile.setDescription(request.description());
    return RoleProfileResponse.from(roleProfiles.save(profile));
  }

  public void deleteRoleProfile(Actor actor, UUID roleProfileId) {
    actor.requireRole("HR_SPECIALIST");
    RoleProfile profile = requireRoleProfile(roleProfileId);
    if (employments.findAll().stream().anyMatch(e -> roleProfileId.equals(e.getRoleProfileId()))) {
      throw new ConflictException("Cannot delete role profile: it is still used by employee assignments.");
    }
    roleProfiles.delete(profile);
  }

  public RoleProfileResponse getRoleProfile(Actor actor, UUID roleProfileId) {
        actor.requireAnyRole("HR_SPECIALIST", "HR_APPROVER");
        return RoleProfileResponse.from(requireRoleProfile(roleProfileId));
    }

    public List<RoleProfileResponse> listRoleProfiles(Actor actor) {
        actor.requireAnyRole("HR_SPECIALIST", "HR_APPROVER");
        List<RoleProfileResponse> result = new ArrayList<>();
        for (RoleProfile roleProfile : roleProfiles.findAll()) {
            result.add(RoleProfileResponse.from(roleProfile));
        }
        return result;
    }

    public RoleProfile requireRoleProfile(UUID roleProfileId) {
        return roleProfiles.findById(roleProfileId)
                .orElseThrow(() -> NotFoundException.forId("Role profile", roleProfileId));
    }
}
