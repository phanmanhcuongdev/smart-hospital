package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import vn.edu.ptit.smarthms.common.domain.AuditedEntity;

/** A diagnosis recorded on an encounter. Maps to emr.diagnosis (FR-EMR-01). */
@Entity
@Table(schema = "emr", name = "diagnosis")
public class Diagnosis extends AuditedEntity {

    public enum Type { PRIMARY, SECONDARY }
    public enum Status { PROVISIONAL, CONFIRMED }

    @Id
    @Column(name = "diagnosis_id", nullable = false)
    private UUID id;

    @Column(name = "encounter_id", nullable = false)
    private UUID encounterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "diagnosis_type", nullable = false, length = 16)
    private Type type;

    @Column(name = "icd10_code", length = 20)
    private String icd10Code;

    @Column(name = "narrative", nullable = false)
    private String narrative;

    @Enumerated(EnumType.STRING)
    @Column(name = "diagnosis_status", nullable = false, length = 16)
    private Status status;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    protected Diagnosis() {
    }

    public Diagnosis(UUID encounterId, Type type, String icd10Code, String narrative) {
        this.id = UUID.randomUUID();
        this.encounterId = encounterId;
        this.type = type;
        this.icd10Code = icd10Code;
        this.narrative = narrative;
        this.status = Status.PROVISIONAL;
    }

    public UUID getId() { return id; }
    public UUID getEncounterId() { return encounterId; }
    public Type getType() { return type; }
    public String getIcd10Code() { return icd10Code; }
    public String getNarrative() { return narrative; }
    public Status getStatus() { return status; }
    public UUID getConfirmedBy() { return confirmedBy; }
    public void setStatus(Status status) { this.status = status; }
    public void setConfirmedBy(UUID confirmedBy) { this.confirmedBy = confirmedBy; }
    public void setIcd10Code(String icd10Code) { this.icd10Code = icd10Code; }
}
