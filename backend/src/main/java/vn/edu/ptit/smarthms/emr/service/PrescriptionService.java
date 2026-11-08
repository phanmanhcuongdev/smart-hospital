package vn.edu.ptit.smarthms.emr.service;

import java.math.BigDecimal;
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
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.repository.PrescriptionItemRepository;
import vn.edu.ptit.smarthms.emr.repository.PrescriptionRepository;

@Service
public class PrescriptionService {
    private static final String DEV_ACTOR = "dev-stub"; // thay bằng user đăng nhập khi V.2 xong
    /** Giả định, cần đối chiếu BR trong III.3/IV.7. */
    private static final Set<EncounterStatus> CAN_PRESCRIBE = Set.of(
            EncounterStatus.IN_PROGRESS, EncounterStatus.PENDING_LAB_RAD, EncounterStatus.DIAGNOSED);

    private final PrescriptionRepository prescriptions;
    private final PrescriptionItemRepository items;
    private final MedicalEncounterRepository encounters;
    private final ReferenceLookupPort lookup;

    public PrescriptionService(PrescriptionRepository prescriptions, PrescriptionItemRepository items,
                               MedicalEncounterRepository encounters, ReferenceLookupPort lookup) {
        this.prescriptions = prescriptions;
        this.items = items;
        this.encounters = encounters;
        this.lookup = lookup;
    }

    @Transactional
    public Prescription create(UUID encounterId) {
        MedicalEncounter e = findEncounter(encounterId);
        if (!CAN_PRESCRIBE.contains(e.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot create prescription when encounter is " + e.getStatus());
        }
        // Tạm dùng bác sĩ phụ trách làm người kê đơn; V.2 thay bằng người dùng đăng nhập.
        return prescriptions.save(Prescription.createDraft(encounterId, e.getAttendingEmployeeId(), DEV_ACTOR));
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

    @Transactional
    public Prescription addItem(UUID encounterId, UUID prescriptionId, UUID medicationId, String dose,
                                String route, String frequency, int daysCount, BigDecimal quantity,
                                String unitCode) {
        findEncounter(encounterId);
        Prescription p = find(encounterId, prescriptionId);
        if (p.getStatus() != Prescription.Status.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot add items when prescription is " + p.getStatus());
        }
        String label = lookup.activeMedicationLabel(medicationId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Medication not found or inactive"));
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
        Prescription p = find(encounterId, prescriptionId);
        if (p.getStatus() != Prescription.Status.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot issue prescription when it is " + p.getStatus());
        }
        if (items.countByIdPrescriptionId(prescriptionId) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prescription has no items");
        }
        p.issue(DEV_ACTOR);
        return p;
    }

    @Transactional
    public Prescription cancel(UUID encounterId, UUID prescriptionId) {
        findEncounter(encounterId);
        Prescription p = find(encounterId, prescriptionId);
        if (!p.canCancel()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot cancel prescription when it is " + p.getStatus());
        }
        p.cancel(DEV_ACTOR);
        return p;
    }

    private MedicalEncounter findEncounter(UUID id) {
        return encounters.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
    }

    private Prescription find(UUID encounterId, UUID prescriptionId) {
        return prescriptions.findById(prescriptionId)
                .filter(x -> x.getEncounterId().equals(encounterId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found"));
    }
}