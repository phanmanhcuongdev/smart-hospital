package vn.edu.ptit.smarthms.emr.api;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort.DepartmentSummary;

/**
 * Mô tả: Controller cung cấp API tra cứu danh mục khoa phòng từ HR phục vụ cho giao diện khám bệnh EMR
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 11/11/2026
 */
@RestController
@RequestMapping("/api/v1/emr/departments")
@PreAuthorize("isAuthenticated()")
public class DepartmentLookupController {
    private final ReferenceLookupPort lookup;

    public DepartmentLookupController(ReferenceLookupPort lookup) {
        this.lookup = lookup;
    }

    /**
     * Mô tả: Lấy danh sách các khoa phòng đang hoạt động phục vụ chọn nơi khám
     * Người thực hiện: Đoàn Thị Hồng Nhung
     * Ngày thực hiện: 11/11/2026
     */
    @GetMapping
    public List<DepartmentSummary> list() {
        return lookup.listDepartments();
    }
}
