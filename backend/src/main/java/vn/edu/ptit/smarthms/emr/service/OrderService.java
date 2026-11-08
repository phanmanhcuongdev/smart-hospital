package vn.edu.ptit.smarthms.emr.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.common.error.ConflictException;
import vn.edu.ptit.smarthms.common.error.ForbiddenException;
import vn.edu.ptit.smarthms.common.error.NotFoundException;
import vn.edu.ptit.smarthms.common.error.ValidationException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.api.EmrRequests.OrderRequest;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.OrderResponse;
import vn.edu.ptit.smarthms.emr.api.EmrRequests;
import vn.edu.ptit.smarthms.emr.api.EmrRequests.ResultRequest;
import vn.edu.ptit.smarthms.emr.api.EmrResponses;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.ResultResponse;
import vn.edu.ptit.smarthms.emr.domain.ClinicalResult;
import vn.edu.ptit.smarthms.emr.domain.ClinicalResultRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalOrder;
import vn.edu.ptit.smarthms.emr.domain.MedicalOrderRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecordRepository;

/** CLS orders and result reconciliation (FR-EMR-02, BR-047 to BR-052, BR-100). */
@Service
@Transactional
public class OrderService {

    private final MedicalEncounterRepository encounters;
    private final MedicalOrderRepository orders;
    private final ClinicalResultRepository results;
    private final MedicalRecordRepository records;
    private final PractitionerPort practitionerPort;

    public OrderService(MedicalEncounterRepository encounters, MedicalOrderRepository orders,
            ClinicalResultRepository results, MedicalRecordRepository records,
            PractitionerPort practitionerPort) {
        this.encounters = encounters;
        this.orders = orders;
        this.results = results;
        this.records = records;
        this.practitionerPort = practitionerPort;
    }

    public OrderResponse createOrder(Actor actor, UUID encounterId, OrderRequest request) {
        actor.requireRole("DOCTOR");
        MedicalEncounter encounter = requireEncounter(encounterId);
        UUID doctorId = requireAttending(actor, encounter);
        requirePrescribingDoctor(doctorId);
        requireActive(encounter);
        requireRecordNotLocked(encounterId);
        MedicalOrder.Type type;
        try {
            type = MedicalOrder.Type.valueOf(request.type());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Unknown order type.");
        }
        MedicalOrder order = new MedicalOrder(encounterId, doctorId, type, request.externalOrderRef());
        order.setCreatedBy(actor.principalName());
        order.setUpdatedBy(actor.principalName());
        return OrderResponse.from(orders.save(order));
    }

    public OrderResponse submitOrder(Actor actor, UUID orderId) {
        MedicalOrder order = requireOrder(orderId);
        MedicalEncounter encounter = requireEncounter(order.getEncounterId());
        requireAttending(actor, encounter);
        if (order.getStatus() != MedicalOrder.Status.DRAFT) {
            throw new ConflictException("Only a DRAFT order can be submitted.");
        }
        order.setStatus(MedicalOrder.Status.ORDERED);
        order.setOrderedAt(Instant.now());
        order.setUpdatedBy(actor.principalName());
        return OrderResponse.from(orders.save(order));
    }

    public OrderResponse acknowledgeOrder(Actor actor, UUID orderId) {
        actor.requireRole("CLS_TECHNICIAN");
        MedicalOrder order = requireOrder(orderId);
        if (order.getStatus() != MedicalOrder.Status.ORDERED) {
            throw new ConflictException("Only an ORDERED order can be acknowledged.");
        }
        order.setStatus(MedicalOrder.Status.ACKNOWLEDGED);
        order.setUpdatedBy(actor.principalName());
        return OrderResponse.from(orders.save(order));
    }

    public OrderResponse startOrder(Actor actor, UUID orderId) {
        actor.requireRole("CLS_TECHNICIAN");
        MedicalOrder order = requireOrder(orderId);
        if (order.getStatus() != MedicalOrder.Status.ACKNOWLEDGED) {
            throw new ConflictException("Only an ACKNOWLEDGED order can be started.");
        }
        order.setStatus(MedicalOrder.Status.IN_PROGRESS);
        order.setUpdatedBy(actor.principalName());
        return OrderResponse.from(orders.save(order));
    }

    public OrderResponse cancelOrder(Actor actor, UUID orderId) {
        MedicalOrder order = requireOrder(orderId);
        MedicalEncounter encounter = requireEncounter(order.getEncounterId());
        requireAttending(actor, encounter);
        if (order.getStatus() == MedicalOrder.Status.COMPLETED
                || order.getStatus() == MedicalOrder.Status.CANCELLED) {
            throw new ConflictException("Order is already closed.");
        }
        order.setStatus(MedicalOrder.Status.CANCELLED);
        order.setUpdatedBy(actor.principalName());
        return OrderResponse.from(orders.save(order));
    }

    public OrderResponse getOrder(Actor actor, UUID orderId) {
        MedicalOrder order = requireOrder(orderId);
        MedicalEncounter encounter = requireEncounter(order.getEncounterId());
        if (actor.hasRole("DOCTOR")) {
            requireAttending(actor, encounter);
        }
        return OrderResponse.from(order);
    }

    public java.util.List<EmrResponses.ResultResponse> listResults(Actor actor, UUID orderId) {
        MedicalOrder order = requireOrder(orderId);
        MedicalEncounter encounter = requireEncounter(order.getEncounterId());
        if (actor.hasRole("DOCTOR")) {
            requireAttending(actor, encounter);
        }
        return results.findByOrderId(orderId).stream()
                .map(r -> EmrResponses.ResultResponse.from(r, false)).toList();
    }

    public EmrResponses.ResultResponse receiveResult(Actor actor, UUID orderId,
            EmrRequests.ResultRequest request) {
        actor.requireRole("CLS_TECHNICIAN");
        MedicalOrder order = requireOrder(orderId);
        MedicalEncounter encounter = requireEncounter(order.getEncounterId());
        if (order.getStatus() == MedicalOrder.Status.DRAFT
                || order.getStatus() == MedicalOrder.Status.CANCELLED) {
            throw new ConflictException("Results cannot be received for a DRAFT or CANCELLED order.");
        }
        var existing = results.findBySourceSystemAndSourceResultIdAndSourceVersion(
                request.sourceSystem(), request.sourceResultId(), request.sourceVersion());
        if (existing.isPresent()) {
            return EmrResponses.ResultResponse.from(existing.get(), true);
        }
        ClinicalResult.Quality quality = ClinicalResult.Quality.VALID;
        if (encounter.getPatientId().equals(request.patientId()) == false) {
            quality = ClinicalResult.Quality.QUARANTINED;
        }
        ClinicalResult result = new ClinicalResult(orderId, request.sourceSystem(),
                request.sourceResultId(), request.sourceVersion(), request.resultPayload(),
                request.abnormalFlag(), quality);
        result.setCreatedBy(actor.principalName());
        result.setUpdatedBy(actor.principalName());
        ClinicalResult saved = results.save(result);
        if (quality == ClinicalResult.Quality.VALID
                && (order.getStatus() == MedicalOrder.Status.ORDERED
                        || order.getStatus() == MedicalOrder.Status.ACKNOWLEDGED
                        || order.getStatus() == MedicalOrder.Status.IN_PROGRESS)) {
            order.setStatus(MedicalOrder.Status.COMPLETED);
            order.setUpdatedBy(actor.principalName());
            orders.save(order);
        }
        return EmrResponses.ResultResponse.from(saved, false);
    }

    public MedicalOrder requireOrder(UUID orderId) {
        return orders.findById(orderId)
                .orElseThrow(() -> NotFoundException.forId("Order", orderId));
    }

    private MedicalEncounter requireEncounter(UUID encounterId) {
        return encounters.findById(encounterId)
                .orElseThrow(() -> NotFoundException.forId("Encounter", encounterId));
    }

    private UUID requireAttending(Actor actor, MedicalEncounter encounter) {
        actor.requireRole("DOCTOR");
        UUID doctorId = practitionerPort.employeeIdForCode(actor.principalName())
                .orElseThrow(() -> new ForbiddenException("Caller has no linked employee record."));
        if (encounter.getAttendingEmployeeId().equals(doctorId) == false) {
            throw new ForbiddenException("Encounter is assigned to another doctor.");
        }
        return doctorId;
    }

    private void requirePrescribingDoctor(UUID doctorId) {
        if (practitionerPort.isActiveEmployee(doctorId) == false) {
            throw new ValidationException("Order must be created by an ACTIVE doctor.");
        }
        if (practitionerPort.hasValidCchn(doctorId, LocalDate.now()) == false) {
            throw new ForbiddenException("Doctor does not hold a valid CCHN.");
        }
    }

    private void requireActive(MedicalEncounter encounter) {
        if (encounter.getStatus() == MedicalEncounter.Status.CANCELLED
                || encounter.getStatus() == MedicalEncounter.Status.COMPLETED
                || encounter.getStatus() == MedicalEncounter.Status.NO_SHOW) {
            throw new ConflictException("Encounter is closed.");
        }
    }

    private void requireRecordNotLocked(UUID encounterId) {
        records.findByEncounterId(encounterId).ifPresent(record -> {
            if (record.getStatus() == MedicalRecord.Status.LOCKED
                    || record.getStatus() == MedicalRecord.Status.ARCHIVED) {
                throw new ConflictException("The medical record is locked.");
            }
        });
    }
}
