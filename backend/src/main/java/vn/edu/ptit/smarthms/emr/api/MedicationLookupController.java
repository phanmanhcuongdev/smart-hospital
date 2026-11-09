package vn.edu.ptit.smarthms.emr.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort.MedicationSummary;

@RestController
@RequestMapping("/api/v1/emr/medications")
public class MedicationLookupController {
    private final ReferenceLookupPort lookup;

    public MedicationLookupController(ReferenceLookupPort lookup) { this.lookup = lookup; }

    @GetMapping
    public List<MedicationSummary> list() { return lookup.listMedications(); }
}