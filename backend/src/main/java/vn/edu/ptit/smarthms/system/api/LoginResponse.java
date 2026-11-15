package vn.edu.ptit.smarthms.system.api;

import java.time.Instant;
import java.util.List;

/**
 * Mô tả: DTO chứa kết quả đăng nhập thành công bao gồm token, hạn dùng, username và danh sách vai trò
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 12/11/2026
 */
public record LoginResponse(
        String token,
        Instant expiresAt,
        String username,
        List<String> roles
) {}
