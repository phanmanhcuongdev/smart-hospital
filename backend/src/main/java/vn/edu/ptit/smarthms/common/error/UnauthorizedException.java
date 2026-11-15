package vn.edu.ptit.smarthms.common.error;

/**
 * Mô tả: Ngoại lệ xử lý lỗi xác thực không thành công hoặc thiếu quyền truy cập (HTTP 401 Unauthorized)
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 12/11/2026
 */
public class UnauthorizedException extends RuntimeException {

    private final String code;

    public UnauthorizedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
