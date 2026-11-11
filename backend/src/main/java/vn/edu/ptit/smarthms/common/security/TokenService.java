package vn.edu.ptit.smarthms.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.system.domain.Session;
import vn.edu.ptit.smarthms.system.domain.UserAccount;
import vn.edu.ptit.smarthms.system.repository.SessionRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final SessionRepository sessionRepository;

    @Value("${app.security.session-ttl-hours:8}")
    private int sessionTtlHours;

    public TokenService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Generate a new random opaque token (UUID v4).
     */
    public String generateToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Compute SHA-256 hex digest of the given raw token.
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Create a new session for the given user, persist it, and return the raw token.
     */
    @Transactional
    public String createSession(UserAccount user) {
        String rawToken = generateToken();
        String digest = hashToken(rawToken);

        Session session = new Session();
        session.setSessionId(UUID.randomUUID());
        session.setUser(user);
        session.setTokenDigest(digest);

        Instant now = Instant.now();
        session.setIssuedAt(now);
        session.setExpiresAt(now.plus(sessionTtlHours, ChronoUnit.HOURS));
        session.setIssuedEpoch(Math.max(0, user.getRevocationEpoch()));

        sessionRepository.save(session);
        log.debug("Created session for user={}", user.getUsername());
        return rawToken;
    }

    /**
     * Look up a session by raw token (including expired or revoked sessions) for detailed status inspection.
     */
    @Transactional(readOnly = true)
    public Optional<Session> findSessionByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        String digest = hashToken(rawToken);
        return sessionRepository.findByTokenDigest(digest);
    }

    /**
     * Validate a raw token: look up by digest, check not expired and not revoked.
     */
    @Transactional(readOnly = true)
    public Optional<Session> validateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        String digest = hashToken(rawToken);
        return sessionRepository.findByTokenDigest(digest)
                .filter(Session::isValid);
    }

    /**
     * Revoke a session by setting revokedAt to now.
     */
    @Transactional
    public void revokeSession(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String digest = hashToken(rawToken);
        sessionRepository.findByTokenDigest(digest).ifPresent(session -> {
            session.setRevokedAt(Instant.now());
            sessionRepository.save(session);
            log.debug("Revoked session for user={}", session.getUser().getUsername());
        });
    }
}
