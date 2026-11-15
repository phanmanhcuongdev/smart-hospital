package vn.edu.ptit.smarthms.system.api;

import java.util.List;

/**
 * Mô tả: DTO trả về thông tin chi tiết của người dùng đang đăng nhập (username, roles, permissions)
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 12/11/2026
 */
public record MeResponse(
        String username,
        List<String> roles,
        List<String> permissions
) {}
