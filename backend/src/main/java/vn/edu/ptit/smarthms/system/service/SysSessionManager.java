package vn.edu.ptit.smarthms.system.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class SysSessionManager {

    public record UserSession(
            UUID sessionId,
            UUID userId,
            String employeeCode,
            String tokenDigest,
            Instant issuedAt,
            Instant expiresAt,
            Instant revokedAt
    ) {
        public boolean isActive() {
            Instant now = Instant.now();
            return revokedAt == null && now.isAfter(issuedAt) && now.isBefore(expiresAt);
        }
    }

    private final Map<UUID, UserSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, UUID> tokenToSessionId = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> employeeToSessions = new ConcurrentHashMap<>();

    public UserSession registerSession(UUID userId, String employeeCode, String tokenDigest,
                                       Instant issuedAt, Instant expiresAt) {
        UUID sessionId = UUID.randomUUID();
        UserSession session = new UserSession(
                sessionId, userId, employeeCode, tokenDigest, issuedAt, expiresAt, null);
        sessionsById.put(sessionId, session);
        tokenToSessionId.put(tokenDigest, sessionId);
        employeeToSessions.computeIfAbsent(employeeCode, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        return session;
    }

    public boolean isSessionValid(UUID sessionId) {
        UserSession session = sessionsById.get(sessionId);
        return session != null && session.isActive();
    }

    public boolean isTokenValid(String tokenDigest) {
        UUID sessionId = tokenToSessionId.get(tokenDigest);
        if (sessionId == null) return false;
        return isSessionValid(sessionId);
    }

    /**
     * Invalidate all sessions of an employee when terminated (TC-INT-007).
     */
    public int invalidateSessionsForEmployee(String employeeCode) {
        Set<UUID> sessionIds = employeeToSessions.get(employeeCode);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return 0;
        }
        int revokedCount = 0;
        Instant now = Instant.now();
        for (UUID sId : sessionIds) {
            UserSession existing = sessionsById.get(sId);
            if (existing != null && existing.revokedAt() == null) {
                UserSession revoked = new UserSession(
                        existing.sessionId(), existing.userId(), existing.employeeCode(),
                        existing.tokenDigest(), existing.issuedAt(), existing.expiresAt(), now);
                sessionsById.put(sId, revoked);
                revokedCount++;
            }
        }
        return revokedCount;
    }

    public Optional<UserSession> getSession(UUID sessionId) {
        return Optional.ofNullable(sessionsById.get(sessionId));
    }

    public void clear() {
        sessionsById.clear();
        tokenToSessionId.clear();
        employeeToSessions.clear();
    }
}
