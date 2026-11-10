package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "emr", name = "addendum")
public class Addendum {
    public enum Status { DRAFT, CONFIRMED }

    @Id @Column(name = "addendum_id") private UUID addendumId;
    @Column(name = "record_id", nullable = false) private UUID recordId;
    @Column(nullable = false, columnDefinition = "text") private String reason;
    @Column(nullable = false, columnDefinition = "text") private String content;
    @Enumerated(EnumType.STRING) @Column(name = "confirmation_status", nullable = false, length = 16)
    private Status confirmationStatus;
    @Column(name = "confirmed_at") private OffsetDateTime confirmedAt;
    @Column(name = "confirmed_by") private UUID confirmedBy;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "created_by", nullable = false, length = 80) private String createdBy;
    @Column(name = "updated_by", nullable = false, length = 80) private String updatedBy;

    protected Addendum() {}

    public static Addendum create(UUID recordId, String reason, String content, String actor) {
        Addendum a = new Addendum();
        OffsetDateTime now = OffsetDateTime.now();
        a.addendumId = UUID.randomUUID();
        a.recordId = recordId;
        a.reason = reason;
        a.content = content;
        a.confirmationStatus = Status.DRAFT;
        a.createdAt = now;
        a.updatedAt = now;
        a.createdBy = actor;
        a.updatedBy = actor;
        return a;
    }

    public void confirm(UUID userId, String actor) {
        OffsetDateTime now = OffsetDateTime.now();
        this.confirmationStatus = Status.CONFIRMED;
        this.confirmedAt = now;
        this.confirmedBy = userId;
        this.updatedAt = now;
        this.updatedBy = actor;
    }

    public UUID getAddendumId() { return addendumId; }
    public UUID getRecordId() { return recordId; }
    public String getReason() { return reason; }
    public String getContent() { return content; }
    public Status getConfirmationStatus() { return confirmationStatus; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public UUID getConfirmedBy() { return confirmedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}