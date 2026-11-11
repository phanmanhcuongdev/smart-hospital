package vn.edu.ptit.smarthms.emr.api;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort.DepartmentSummary;

@RestController
@RequestMapping("/api/v1/emr/departments")
@PreAuthorize("isAuthenticated()")
public class DepartmentLookupController {
    private final ReferenceLookupPort lookup;

    public DepartmentLookupController(ReferenceLookupPort lookup) {
        this.lookup = lookup;
    }

    @GetMapping
    public List<DepartmentSummary> list() {
        return lookup.listDepartments();
    }
}
