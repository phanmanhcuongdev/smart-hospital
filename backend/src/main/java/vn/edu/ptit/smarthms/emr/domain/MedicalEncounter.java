package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "emr", name = "medical_encounter")
public class MedicalEncounter {
    @Id
    @Column(name = "encounter_id")
    private UUID encounterId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "appointment_id")
    private UUID appointmentId;

    @Column(name = "attending_employee_id", nullable = false)
    private UUID attendingEmployeeId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EncounterStatus status;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "row_version", nullable = false)
    private int rowVersion;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 80)
    private String updatedBy;

    protected MedicalEncounter() {
    }

    public static MedicalEncounter create(UUID patientId, UUID appointmentId,
            UUID attendingEmployeeId, UUID departmentId, String actor) {
        MedicalEncounter e = new MedicalEncounter();
        OffsetDateTime now = OffsetDateTime.now();
        e.encounterId = UUID.randomUUID();
        e.patientId = patientId;
        e.appointmentId = appointmentId;
        e.attendingEmployeeId = attendingEmployeeId;
        e.departmentId = departmentId;
        e.status = EncounterStatus.WAITING;
        e.rowVersion = 1;
        e.createdAt = now;
        e.updatedAt = now;
        e.createdBy = actor;
        e.updatedBy = actor;
        return e;
    }

    public static MedicalEncounter create(UUID patientId, UUID appointmentId,
            UUID attendingEmployeeId, String actor) {
        return create(patientId, appointmentId, attendingEmployeeId, null, actor);
    }

    public void changeStatus(EncounterStatus next, String actor, OffsetDateTime now) {
        this.status = next;
        if (next == EncounterStatus.IN_PROGRESS && this.startedAt == null) {
            this.startedAt = now;
        }
        if (next == EncounterStatus.COMPLETED) {
            this.completedAt = now;
        }
        this.rowVersion++;
        this.updatedAt = now;
        this.updatedBy = actor;
    }

    public void assignPractitioner(UUID attendingEmployeeId, String actor, OffsetDateTime now) {
        if (attendingEmployeeId != null && !attendingEmployeeId.equals(this.attendingEmployeeId)) {
            this.attendingEmployeeId = attendingEmployeeId;
            this.rowVersion++;
            this.updatedAt = now;
            this.updatedBy = actor;
        }
    }

    public void assignDepartment(UUID departmentId, String actor, OffsetDateTime now) {
        if (departmentId != null && !departmentId.equals(this.departmentId)) {
            this.departmentId = departmentId;
            this.rowVersion++;
            this.updatedAt = now;
            this.updatedBy = actor;
        }
    }

    public UUID getEncounterId() {
        return encounterId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public UUID getAttendingEmployeeId() {
        return attendingEmployeeId;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public EncounterStatus getStatus() {
        return status;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public int getRowVersion() {
        return rowVersion;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
