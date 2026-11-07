package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.edu.ptit.smarthms.common.domain.AuditedEntity;

/** One immutable snapshot of a record. Maps to emr.record_version (FR-EMR-04). */
@Entity
@Table(schema = "emr", name = "record_version")
public class RecordVersion extends AuditedEntity {

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "record_id", nullable = false)
        private UUID recordId;
        @Column(name = "version_no", nullable = false)
        private Integer versionNo;
        protected Key() {
        }
        public Key(UUID recordId, Integer versionNo) {
            this.recordId = recordId;
            this.versionNo = versionNo;
        }
        public UUID getRecordId() { return recordId; }
        public Integer getVersionNo() { return versionNo; }
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            Key key = (Key) other;
            return Objects.equals(recordId, key.recordId) && Objects.equals(versionNo, key.versionNo);
        }
        @Override
        public int hashCode() {
            return Objects.hash(recordId, versionNo);
        }
    }

    @EmbeddedId
    private Key id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "sha256_hex", nullable = false, length = 64)
    private String sha256Hex;

    @Column(name = "version_created_at", nullable = false)
    private Instant versionCreatedAt;

    protected RecordVersion() {
    }

    public RecordVersion(UUID recordId, Integer versionNo, String payload, String sha256Hex) {
        this.id = new Key(recordId, versionNo);
        this.payload = payload;
        this.sha256Hex = sha256Hex;
        this.versionCreatedAt = Instant.now();
    }

    public Key getId() { return id; }
    public UUID getRecordId() { return id.getRecordId(); }
    public Integer getVersionNo() { return id.getVersionNo(); }
    public String getPayload() { return payload; }
    public String getSha256Hex() { return sha256Hex; }
    public Instant getVersionCreatedAt() { return versionCreatedAt; }
}
