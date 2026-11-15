package vn.edu.ptit.smarthms.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.ptit.smarthms.system.domain.Session;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Mô tả: Repository quản lý phiên làm việc, tìm kiếm phiên theo mã băm token và xóa các phiên đã hết hạn
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 12/11/2026
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByTokenDigest(String tokenDigest);

    @Modifying
    @Query("DELETE FROM Session s WHERE s.expiresAt < :now")
    void deleteByExpiresAtBefore(Instant now);
}
