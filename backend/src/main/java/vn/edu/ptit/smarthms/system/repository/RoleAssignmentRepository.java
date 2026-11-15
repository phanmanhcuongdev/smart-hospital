package vn.edu.ptit.smarthms.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ptit.smarthms.system.domain.RoleAssignment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mô tả: Repository truy vấn các vai trò đang có hiệu lực theo ID người dùng và thời điểm hiện tại
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 12/11/2026
 */
@Repository
public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, UUID> {

    @Query("SELECT ra FROM RoleAssignment ra " +
           "JOIN FETCH ra.role r " +
           "WHERE ra.user.userId = :userId " +
           "AND ra.startAt <= :now " +
           "AND (ra.endAt IS NULL OR ra.endAt > :now)")
    List<RoleAssignment> findActiveByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
