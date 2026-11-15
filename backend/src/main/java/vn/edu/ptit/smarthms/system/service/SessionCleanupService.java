package vn.edu.ptit.smarthms.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.system.repository.SessionRepository;

import java.time.Instant;

/**
 * Mô tả: Tiến trình chạy ngầm định kỳ (Scheduled Job) tự động dọn dẹp các phiên làm việc (Session) đã hết hạn
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 12/11/2026
 */
@Service
public class SessionCleanupService {

    private static final Logger log = LoggerFactory.getLogger(SessionCleanupService.class);

    private final SessionRepository sessionRepository;

    public SessionCleanupService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Scheduled(fixedDelayString = "PT1H")
    @Transactional
    public void cleanupExpiredSessions() {
        Instant now = Instant.now();
        log.info("Running session cleanup at {}", now);
        try {
            sessionRepository.deleteByExpiresAtBefore(now);
            log.info("Session cleanup completed");
        } catch (Exception e) {
            log.error("Session cleanup failed", e);
        }
    }
}
