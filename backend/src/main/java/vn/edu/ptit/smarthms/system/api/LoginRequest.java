package vn.edu.ptit.smarthms.system.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Mô tả: DTO chứa thông tin yêu cầu đăng nhập của người dùng
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 12/11/2026
 */
public record LoginRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password
) {}
