package vn.edu.ptit.smarthms.emr.api;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort.PatientSummary;

@RestController
@RequestMapping("/api/v1/emr/patients")
@PreAuthorize("isAuthenticated()")
public class PatientLookupController {
    private final ReferenceLookupPort lookup;

    public PatientLookupController(ReferenceLookupPort lookup) { this.lookup = lookup; }

    @GetMapping
    public List<PatientSummary> search(@RequestParam(required = false, defaultValue = "") String q) {
        if (q.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query is too long");
        }
        return lookup.searchPatients(q.trim());
    }
}
