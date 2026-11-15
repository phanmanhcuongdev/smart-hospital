package vn.edu.ptit.smarthms.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ptit.smarthms.system.domain.UserAccount;

import java.util.Optional;
import java.util.UUID;

/**
 * Mô tả: Repository truy xuất dữ liệu tài khoản người dùng theo username hoặc ID
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 12/11/2026
 */
@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByUsername(String username);
}
