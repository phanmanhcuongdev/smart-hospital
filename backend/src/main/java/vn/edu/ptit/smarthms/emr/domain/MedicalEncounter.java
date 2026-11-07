package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import vn.edu.ptit.smarthms.common.domain.AuditedEntity;

/** One examination visit. Maps to emr.medical_encounter (FR-EMR-01). */
@Entity
@Table(schema = "emr", name = "medical_encounter")
public class MedicalEncounter extends AuditedEntity {

    public enum Status {
        WAITING, IN_PROGRESS, PENDING_LAB_RAD, DIAGNOSED, COMPLETED, CANCELLED, NO_SHOW
    }

    @Id
    @Column(name = "encounter_id", nullable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "appointment_id")
    private UUID appointmentId;

    @Column(name = "attending_employee_id", nullable = false)
    private UUID attendingEmployeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private Integer rowVersion = 1;

    protected MedicalEncounter() {
    }

    public MedicalEncounter(UUID patientId, UUID appointmentId, UUID attendingEmployeeId) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.appointmentId = appointmentId;
        this.attendingEmployeeId = attendingEmployeeId;
        this.status = Status.WAITING;
    }

    public UUID getId() { return id; }
    public UUID getPatientId() { return patientId; }
    public UUID getAppointmentId() { return appointmentId; }
    public UUID getAttendingEmployeeId() { return attendingEmployeeId; }
    public Status getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Integer getRowVersion() { return rowVersion; }
    public void setStatus(Status status) { this.status = status; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
