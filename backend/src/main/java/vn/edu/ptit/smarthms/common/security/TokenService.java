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

/**
 * Mô tả: Dịch vụ quản lý Bearer token và phiên đăng nhập (Session) - tạo token, băm SHA-256 lưu CSDL, xác thực và thu hồi token
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 12/11/2026
 */
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
     * Mô tả: Tạo chuỗi token ngẫu nhiên dạng UUID v4
     * Người thực hiện: Đoàn Thị Hồng Nhung
     * Ngày thực hiện: 12/11/2026
     */
    public String generateToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Mô tả: Băm chuỗi token bằng thuật toán SHA-256 để lưu trữ an toàn trong CSDL
     * Người thực hiện: Đoàn Thị Hồng Nhung
     * Ngày thực hiện: 12/11/2026
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
     * Mô tả: Tạo phiên làm việc mới (Session) cho người dùng, lưu bản băm token và trả về raw token
     * Người thực hiện: Đoàn Thị Hồng Nhung
     * Ngày thực hiện: 12/11/2026
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
     * Mô tả: Xác thực token - tra cứu phiên làm việc qua mã băm và kiểm tra còn hạn / chưa bị thu hồi
     * Người thực hiện: Đoàn Thị Hồng Nhung
     * Ngày thực hiện: 12/11/2026
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
     * Mô tả: Thu hồi phiên làm việc (Revoke session) khi người dùng đăng xuất hoặc bị vô hiệu hóa
     * Người thực hiện: Đoàn Thị Hồng Nhung
     * Ngày thực hiện: 12/11/2026
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
