/**
 * Mô tả: Đường dẫn liệt kê tài khoản cho thao tác khóa hồ sơ; chỉ dùng tạm, xóa khi có đăng nhập.
 * Người thực hiện: Trần Duy Hiếu
 * Ngày thực hiện: 09/11/2026
 */
package vn.edu.ptit.smarthms.emr.api;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort.UserSummary;

@RestController
@RequestMapping("/api/v1/emr/users")
@PreAuthorize("isAuthenticated()")
public class UserLookupController {
    private final ReferenceLookupPort lookup;

    public UserLookupController(ReferenceLookupPort lookup) { this.lookup = lookup; }

    @GetMapping
    public List<UserSummary> list() { return lookup.listUsers(); }
}
