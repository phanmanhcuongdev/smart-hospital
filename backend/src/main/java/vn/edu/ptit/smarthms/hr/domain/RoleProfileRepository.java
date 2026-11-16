/**
 * Mô tả: Repository JPA truy vấn dữ liệu hồ sơ vai trò của nhân viên
 * Người thực hiện: Tran Hoang Nam
 * Ngày thực hiện: 19/10/2026
 */

package vn.edu.ptit.smarthms.hr.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleProfileRepository extends JpaRepository<RoleProfile, UUID> {
    Optional<RoleProfile> findByTitleName(String titleName);
    boolean existsByTitleName(String titleName);
}
