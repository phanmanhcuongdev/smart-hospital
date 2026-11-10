package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "emr", name = "diagnosis")
public class Diagnosis {
    @Id @Column(name = "diagnosis_id") private UUID diagnosisId;
    @Column(name = "encounter_id", nullable = false) private UUID encounterId;
    @Enumerated(EnumType.STRING) @Column(name = "diagnosis_type", nullable = false, length = 16)
    private DiagnosisType diagnosisType;
    @Column(name = "icd10_code", length = 20) private String icd10Code;
    @Column(nullable = false, columnDefinition = "text") private String narrative;
    @Enumerated(EnumType.STRING) @Column(name = "diagnosis_status", nullable = false, length = 16)
    private DiagnosisStatus diagnosisStatus;
    @Column(name = "confirmed_by") private UUID confirmedBy;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "created_by", nullable = false, length = 80) private String createdBy;
    @Column(name = "updated_by", nullable = false, length = 80) private String updatedBy;

    protected Diagnosis() {}

    public static Diagnosis create(UUID encounterId, DiagnosisType type, String icd10Code,
                                   String narrative, String actor) {
        Diagnosis d = new Diagnosis();
        OffsetDateTime now = OffsetDateTime.now();
        d.diagnosisId = UUID.randomUUID();
        d.encounterId = encounterId;
        d.diagnosisType = type;
        d.icd10Code = icd10Code;
        d.narrative = narrative;
        d.diagnosisStatus = DiagnosisStatus.PROVISIONAL;
        d.createdAt = now;
        d.updatedAt = now;
        d.createdBy = actor;
        d.updatedBy = actor;
        return d;
    }

    public void confirm(UUID employeeId, String actor) {
        this.diagnosisStatus = DiagnosisStatus.CONFIRMED;
        this.confirmedBy = employeeId;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actor;
    }

    public UUID getDiagnosisId() { return diagnosisId; }
    public UUID getEncounterId() { return encounterId; }
    public DiagnosisType getDiagnosisType() { return diagnosisType; }
    public String getIcd10Code() { return icd10Code; }
    public String getNarrative() { return narrative; }
    public DiagnosisStatus getDiagnosisStatus() { return diagnosisStatus; }
    public UUID getConfirmedBy() { return confirmedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}