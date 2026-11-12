package vn.edu.ptit.smarthms.hr.domain;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import vn.edu.ptit.smarthms.common.domain.AuditedEntity;

/** Role profile (hr.role_profiles, FR-HR-01, FR-HR-04). Title is not a system role (BR-015). */
@Entity
@Table(name = "role_profiles", schema = "hr")
public class RoleProfile extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "role_profile_id")
    private UUID id;

    @Column(name = "title_name", nullable = false, unique = true, length = 120)
    private String titleName;

    @Column(name = "professional_group", length = 80)
    private String professionalGroup;

    @Column(name = "qualification", length = 160)
    private String qualification;

    @Column(name = "description")
    private String description;

    protected RoleProfile() {
    }

    public RoleProfile(String titleName) {
        this.titleName = titleName;
    }

    public UUID getId() {
        return id;
    }

    public String getTitleName() {
        return titleName;
    }

    public void setTitleName(String titleName) {
        this.titleName = titleName;
    }

    public String getProfessionalGroup() {
        return professionalGroup;
    }

    public void setProfessionalGroup(String professionalGroup) {
        this.professionalGroup = professionalGroup;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
