package vn.edu.ptit.smarthms.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

/**
 * Mô tả: Đối tượng UserDetails mở rộng của Spring Security, lưu trữ thêm thông tin UUID của tài khoản người dùng
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 12/11/2026
 */
public class SmartHmsUserDetails extends User {

    private final UUID userId;

    public SmartHmsUserDetails(String username, String password, UUID userId,
                               Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
