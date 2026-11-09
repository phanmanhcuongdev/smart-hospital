package vn.edu.ptit.smarthms.system.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "session", schema = "sys")
public class Session extends AuditableEntity {

    @Id
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "token_digest", nullable = false, unique = true, length = 128)
    private String tokenDigest;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "issued_epoch", nullable = false)
    private int issuedEpoch;

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
    public String getTokenDigest() { return tokenDigest; }
    public void setTokenDigest(String tokenDigest) { this.tokenDigest = tokenDigest; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public int getIssuedEpoch() { return issuedEpoch; }
    public void setIssuedEpoch(int issuedEpoch) { this.issuedEpoch = issuedEpoch; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isValid() {
        boolean epochValid = (user == null) || (issuedEpoch == user.getRevocationEpoch());
        return !isExpired() && !isRevoked() && epochValid;
    }
}
