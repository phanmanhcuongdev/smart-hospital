package vn.edu.ptit.smarthms.emr.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.Prescription;
import vn.edu.ptit.smarthms.emr.domain.PrescriptionItem;
import vn.edu.ptit.smarthms.emr.port.AuditPort;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort.AllergyFact;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.repository.PrescriptionItemRepository;
import vn.edu.ptit.smarthms.emr.repository.PrescriptionRepository;

@Service
public class PrescriptionService {
    public record ItemInput(UUID medicationId, String dose, String route, String frequency,
                            int daysCount, BigDecimal quantity, String unitCode) {}

    public record Warning(String code, String message) {}

    private static final String DEV_ACTOR = "dev-stub"; // thay bằng user đăng nhập khi V.2 xong
    private static final String TARGET = "PRESCRIPTION";
    private static final Set<EncounterStatus> CAN_PRESCRIBE = Set.of(
            EncounterStatus.IN_PROGRESS, EncounterStatus.PENDING_LAB_RAD, EncounterStatus.DIAGNOSED);

    private final PrescriptionRepository prescriptions;
    private final PrescriptionItemRepository items;
    private final MedicalEncounterRepository encounters;
    private final ReferenceLookupPort lookup;
    private final RecordLockGuard lockGuard;
    private final AuditPort audit;

    public PrescriptionService(PrescriptionRepository prescriptions, PrescriptionItemRepository items,
                               MedicalEncounterRepository encounters, ReferenceLookupPort lookup,
                               RecordLockGuard lockGuard, AuditPort audit) {
        this.prescriptions = prescriptions;
        this.items = items;
        this.encounters = encounters;
        this.lookup = lookup;
        this.lockGuard = lockGuard;
        this.audit = audit;
    }

    /** IV7 M-09 / API-03: đơn và toàn bộ dòng thuốc được lưu nguyên tử; dòng sai thì không lưu gì. */
    @Transactional
    public Prescription create(UUID encounterId, List<ItemInput> inputs) {
        MedicalEncounter e = findEncounter(encounterId);
        lockGuard.assertEditable(encounterId);
        if (!CAN_PRESCRIBE.contains(e.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot create prescription when encounter is " + e.getStatus());
        }
        if (!lookup.practitionerActive(e.getAttendingEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Prescriber is not ACTIVE");
        }
        if (inputs == null || inputs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one prescription item is required");
        }
        List<String> labels = new ArrayList<>();
        for (ItemInput in : inputs) {
            labels.add(medicationLabel(in.medicationId()));
        }
        // Tạm dùng bác sĩ phụ trách làm người kê đơn; V.2 thay bằng người dùng đăng nhập.
        Prescription p = prescriptions.save(
                Prescription.createDraft(encounterId, e.getAttendingEmployeeId(), DEV_ACTOR));
        try {
            int lineNo = 1;
            for (int i = 0; i < inputs.size(); i++) {
                ItemInput in = inputs.get(i);
                items.saveAndFlush(PrescriptionItem.create(p.getPrescriptionId(), lineNo++, in.medicationId(),
                        labels.get(i), in.dose().trim(), in.route().trim(), in.frequency().trim(),
                        in.daysCount(), in.quantity(), in.unitCode().trim(), DEV_ACTOR));
            }
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Invalid prescription line; nothing was saved");
        }
        audit.success("PRESCRIPTION_CREATED", TARGET, p.getPrescriptionId(), null);
        return p;
    }

    @Transactional(readOnly = true)
    public List<Prescription> list(UUID encounterId) {
        findEncounter(encounterId);
        return prescriptions.findByEncounterIdOrderByCreatedAtAsc(encounterId);
    }

    @Transactional(readOnly = true)
    public Prescription get(UUID encounterId, UUID prescriptionId) {
        findEncounter(encounterId);
        return find(encounterId, prescriptionId);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionItem> itemsOf(UUID prescriptionId) {
        return items.findByIdPrescriptionIdOrderByIdLineNoAsc(prescriptionId);
    }

    /**
     * Cảnh báo dị ứng (IV7 V-06): các trạng thái KNOWN / UNKNOWN / NONE_CONFIRMED không bị gộp.
     * Chính sách xử lý còn OPEN-08 nên chỉ cảnh báo, không chặn kê hay phát hành đơn.
     */
    @Transactional(readOnly = true)
    public List<Warning> warningsFor(Prescription p) {
        MedicalEncounter e = findEncounter(p.getEncounterId());
        List<AllergyFact> facts = lookup.allergiesOf(e.getPatientId());
        List<Warning> out = new ArrayList<>();
        boolean unknown = facts.isEmpty() || facts.stream().anyMatch(f -> "UNKNOWN".equals(f.allergyStatus()));
        if (unknown) {
            out.add(new Warning("ALLERGY_UNKNOWN", "Allergy information is unknown or unverified"));
        }
        List<PrescriptionItem> lines = items.findByIdPrescriptionIdOrderByIdLineNoAsc(p.getPrescriptionId());
        for (AllergyFact f : facts) {
            if (!"HAS_ALLERGY".equals(f.allergyStatus()) || f.allergen() == null || f.allergen().isBlank()) {
                continue;
            }
            String allergen = f.allergen().trim().toLowerCase();
            for (PrescriptionItem line : lines) {
                String label = line.getMedicationLabel().toLowerCase();
                if (label.contains(allergen) || allergen.contains(label)) {
                    out.add(new Warning("ALLERGY_MATCH",
                            line.getMedicationLabel() + " may match the recorded allergen " + f.allergen()));
                }
            }
        }
        return out;
    }

    @Transactional
    public Prescription addItem(UUID encounterId, UUID prescriptionId, UUID medicationId, String dose,
                                String route, String frequency, int daysCount, BigDecimal quantity,
                                String unitCode) {
        findEncounter(encounterId);
        Prescription p = findForUpdate(encounterId, prescriptionId);
        if (p.getStatus() != Prescription.Status.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot add items when prescription is " + p.getStatus());
        }
        String label = medicationLabel(medicationId);
        int lineNo = (int) items.countByIdPrescriptionId(prescriptionId) + 1;
        try {
            items.saveAndFlush(PrescriptionItem.create(prescriptionId, lineNo, medicationId, label,
                    dose.trim(), route.trim(), frequency.trim(), daysCount, quantity, unitCode.trim(), DEV_ACTOR));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not add item, please retry");
        }
        return p;
    }

    @Transactional
    public Prescription issue(UUID encounterId, UUID prescriptionId) {
        findEncounter(encounterId);
        Prescription p = findForUpdate(encounterId, prescriptionId);
        if (p.getStatus() != Prescription.Status.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot issue prescription when it is " + p.getStatus());
        }
        if (items.countByIdPrescriptionId(prescriptionId) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prescription has no items");
        }
        p.issue(DEV_ACTOR);
        audit.success("PRESCRIPTION_ISSUED", TARGET, p.getPrescriptionId(), null);
        return p;
    }

    @Transactional
    public Prescription cancel(UUID encounterId, UUID prescriptionId) {
        findEncounter(encounterId);
        Prescription p = findForUpdate(encounterId, prescriptionId);
        if (!p.canCancel()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot cancel prescription when it is " + p.getStatus());
        }
        p.cancel(DEV_ACTOR);
        return p;
    }

    private String medicationLabel(UUID medicationId) {
        return lookup.activeMedicationLabel(medicationId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Medication not found or inactive"));
    }

    private MedicalEncounter findEncounter(UUID id) {
        return encounters.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
    }

    /** Dùng cho thao tác ghi: chặn khi hồ sơ đã nộp/khóa. */
    private Prescription findForUpdate(UUID encounterId, UUID prescriptionId) {
        lockGuard.assertEditable(encounterId);
        return find(encounterId, prescriptionId);
    }

    private Prescription find(UUID encounterId, UUID prescriptionId) {
        return prescriptions.findById(prescriptionId)
                .filter(x -> x.getEncounterId().equals(encounterId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found"));
    }
}