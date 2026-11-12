package vn.edu.ptit.smarthms.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ptit.smarthms.system.domain.Session;
import vn.edu.ptit.smarthms.system.domain.UserAccount;
import vn.edu.ptit.smarthms.system.repository.SessionRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(sessionRepository);
        // Inject sessionTtlHours via reflection (default 8)
        try {
            var field = TokenService.class.getDeclaredField("sessionTtlHours");
            field.setAccessible(true);
            field.set(tokenService, 8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void generateToken_shouldReturnNonBlankUuid() {
        String token = tokenService.generateToken();
        assertThat(token).isNotBlank();
        // Should parse as UUID
        assertThat(UUID.fromString(token)).isNotNull();
    }

    @Test
    void hashToken_shouldReturnConsistentSha256HexDigest() {
        String token = "test-token-value";
        String hash1 = tokenService.hashToken(token);
        String hash2 = tokenService.hashToken(token);
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 hex = 64 chars
    }

    @Test
    void hashToken_differentInputs_shouldProduceDifferentHashes() {
        String hash1 = tokenService.hashToken("token-a");
        String hash2 = tokenService.hashToken("token-b");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void createSession_shouldPersistSessionAndReturnToken() {
        UserAccount user = new UserAccount();
        user.setUserId(UUID.randomUUID());
        user.setUsername("testuser");

        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

        String rawToken = tokenService.createSession(user);

        assertThat(rawToken).isNotBlank();
        verify(sessionRepository, times(1)).save(any(Session.class));
    }

    @Test
    void validateToken_validToken_shouldReturnSession() {
        String rawToken = UUID.randomUUID().toString();
        String digest = tokenService.hashToken(rawToken);

        UserAccount user = new UserAccount();
        user.setUserId(UUID.randomUUID());
        user.setUsername("testuser");

        Session session = new Session();
        session.setSessionId(UUID.randomUUID());
        session.setUser(user);
        session.setTokenDigest(digest);
        session.setIssuedAt(Instant.now());
        session.setExpiresAt(Instant.now().plusSeconds(3600));

        when(sessionRepository.findByTokenDigest(digest)).thenReturn(Optional.of(session));

        Optional<Session> result = tokenService.validateToken(rawToken);

        assertThat(result).isPresent();
        assertThat(result.get().getTokenDigest()).isEqualTo(digest);
    }

    @Test
    void validateToken_expiredToken_shouldReturnEmpty() {
        String rawToken = UUID.randomUUID().toString();
        String digest = tokenService.hashToken(rawToken);

        UserAccount user = new UserAccount();
        user.setUserId(UUID.randomUUID());

        Session session = new Session();
        session.setSessionId(UUID.randomUUID());
        session.setUser(user);
        session.setTokenDigest(digest);
        session.setIssuedAt(Instant.now().minusSeconds(7200));
        session.setExpiresAt(Instant.now().minusSeconds(3600)); // already expired

        when(sessionRepository.findByTokenDigest(digest)).thenReturn(Optional.of(session));

        Optional<Session> result = tokenService.validateToken(rawToken);
        assertThat(result).isEmpty();
    }

    @Test
    void validateToken_revokedToken_shouldReturnEmpty() {
        String rawToken = UUID.randomUUID().toString();
        String digest = tokenService.hashToken(rawToken);

        UserAccount user = new UserAccount();
        user.setUserId(UUID.randomUUID());

        Session session = new Session();
        session.setSessionId(UUID.randomUUID());
        session.setUser(user);
        session.setTokenDigest(digest);
        session.setIssuedAt(Instant.now());
        session.setExpiresAt(Instant.now().plusSeconds(3600));
        session.setRevokedAt(Instant.now()); // revoked

        when(sessionRepository.findByTokenDigest(digest)).thenReturn(Optional.of(session));

        Optional<Session> result = tokenService.validateToken(rawToken);
        assertThat(result).isEmpty();
    }

    @Test
    void revokeSession_shouldSetRevokedAt() {
        String rawToken = UUID.randomUUID().toString();
        String digest = tokenService.hashToken(rawToken);

        UserAccount user = new UserAccount();
        user.setUserId(UUID.randomUUID());
        user.setUsername("testuser");

        Session session = new Session();
        session.setSessionId(UUID.randomUUID());
        session.setUser(user);
        session.setTokenDigest(digest);
        session.setIssuedAt(Instant.now());
        session.setExpiresAt(Instant.now().plusSeconds(3600));

        when(sessionRepository.findByTokenDigest(digest)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

        tokenService.revokeSession(rawToken);

        assertThat(session.getRevokedAt()).isNotNull();
        verify(sessionRepository).save(session);
    }

    @Test
    void validateToken_nullToken_shouldReturnEmpty() {
        Optional<Session> result = tokenService.validateToken(null);
        assertThat(result).isEmpty();
        verifyNoInteractions(sessionRepository);
    }
}
