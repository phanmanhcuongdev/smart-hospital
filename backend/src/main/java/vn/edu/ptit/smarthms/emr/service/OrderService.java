package vn.edu.ptit.smarthms.emr.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalOrder;
import vn.edu.ptit.smarthms.emr.port.AuditPort;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalOrderRepository;

@Service
public class OrderService {
    private static final String DEV_ACTOR = "dev-stub"; // thay bằng user đăng nhập khi V.2 xong
    private static final Set<EncounterStatus> CAN_ORDER =
            Set.of(EncounterStatus.IN_PROGRESS, EncounterStatus.PENDING_LAB_RAD);

    private final MedicalOrderRepository orders;
    private final MedicalEncounterRepository encounters;
    private final RecordLockGuard lockGuard;
    private final ReferenceLookupPort lookup;
    private final AuditPort audit;

    public OrderService(MedicalOrderRepository orders, MedicalEncounterRepository encounters,
                        RecordLockGuard lockGuard, ReferenceLookupPort lookup, AuditPort audit) {
        this.orders = orders;
        this.encounters = encounters;
        this.lockGuard = lockGuard;
        this.lookup = lookup;
        this.audit = audit;
    }

    @Transactional
    public MedicalOrder create(UUID encounterId, MedicalOrder.Type type, String externalOrderRef) {
        MedicalEncounter e = findEncounter(encounterId);
        if (!CAN_ORDER.contains(e.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot create order when encounter is " + e.getStatus());
        }
        if (!lookup.practitionerActive(e.getAttendingEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ordering practitioner is not ACTIVE");
        }
        // Tạm dùng bác sĩ phụ trách làm người chỉ định; V.2 thay bằng người dùng đăng nhập.
        MedicalOrder saved = orders.save(MedicalOrder.create(
                encounterId, e.getAttendingEmployeeId(), type, externalOrderRef, DEV_ACTOR));
        audit.success("ORDER_CREATED", "MEDICAL_ORDER", saved.getOrderId(), null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<MedicalOrder> list(UUID encounterId) {
        findEncounter(encounterId);
        return orders.findByEncounterIdOrderByCreatedAtAsc(encounterId);
    }

    @Transactional
    public MedicalOrder changeStatus(UUID encounterId, UUID orderId, MedicalOrder.Status next) {
        findEncounter(encounterId);
        lockGuard.assertEditable(encounterId);
        MedicalOrder o = orders.findById(orderId)
                .filter(x -> x.getEncounterId().equals(encounterId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!o.getOrderStatus().allowedNext().contains(next)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot change order status from " + o.getOrderStatus() + " to " + next);
        }
        o.changeStatus(next, DEV_ACTOR);
        return o;
    }

    private MedicalEncounter findEncounter(UUID id) {
        return encounters.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
    }
}