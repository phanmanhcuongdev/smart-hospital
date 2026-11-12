package vn.edu.ptit.smarthms.emr.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalOrder;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalOrderRepository;

@Service
public class OrderService {
    /** Giả định, cần đối chiếu BR trong III.3/IV.7. */
    private static final Set<EncounterStatus> CAN_ORDER =
            Set.of(EncounterStatus.IN_PROGRESS, EncounterStatus.PENDING_LAB_RAD);

    private final MedicalOrderRepository orders;
    private final MedicalEncounterRepository encounters;
    private final RecordLockGuard lockGuard;

    public OrderService(MedicalOrderRepository orders, MedicalEncounterRepository encounters,
                        RecordLockGuard lockGuard) {
        this.orders = orders;
        this.encounters = encounters;
        this.lockGuard = lockGuard;
    }

    @Transactional
    public MedicalOrder create(Actor actor, UUID encounterId, MedicalOrder.Type type, String externalOrderRef) {
        actor.requireAnyRole("DOCTOR", "ADMIN");
        MedicalEncounter e = findEncounter(encounterId);
        if (!CAN_ORDER.contains(e.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot create order when encounter is " + e.getStatus());
        }
        return orders.save(MedicalOrder.create(
                encounterId, e.getAttendingEmployeeId(), type, externalOrderRef, actor.principalName()));
    }

    @Transactional(readOnly = true)
    public List<MedicalOrder> list(UUID encounterId) {
        findEncounter(encounterId);
        return orders.findByEncounterIdOrderByCreatedAtAsc(encounterId);
    }

    @Transactional
    public MedicalOrder changeStatus(Actor actor, UUID encounterId, UUID orderId, MedicalOrder.Status next) {
        actor.requireAnyRole("DOCTOR", "CLS_TECHNICIAN", "ADMIN");
        findEncounter(encounterId);
        lockGuard.assertEditable(encounterId);
        MedicalOrder o = orders.findById(orderId)
                .filter(x -> x.getEncounterId().equals(encounterId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!o.getOrderStatus().allowedNext().contains(next)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot change order status from " + o.getOrderStatus() + " to " + next);
        }
        o.changeStatus(next, actor.principalName());
        return o;
    }

    private MedicalEncounter findEncounter(UUID id) {
        return encounters.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
    }
}
