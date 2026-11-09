package vn.edu.ptit.smarthms.emr.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort.PractitionerSummary;

@RestController
@RequestMapping("/api/v1/emr/practitioners")
public class PractitionerLookupController {
    private final ReferenceLookupPort lookup;

    public PractitionerLookupController(ReferenceLookupPort lookup) { this.lookup = lookup; }

    @GetMapping
    public List<PractitionerSummary> list() { return lookup.listPractitioners(); }
}