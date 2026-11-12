package vn.edu.ptit.smarthms.emr.service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.emr.domain.ClinicalResult;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalOrder;
import vn.edu.ptit.smarthms.emr.repository.ClinicalResultRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalOrderRepository;

@Service
public class ClsIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(ClsIntegrationService.class);

    public record ClsEventMessage(
            String eventId,
            String sourceSystem,
            String sourceResultId,
            String sourceVersion,
            UUID orderId,
            UUID patientId,
            OffsetDateTime resultAt,
            String payload,
            Boolean abnormalFlag
    ) {}

    public record IngestOutcome(
            boolean accepted,
            boolean quarantined,
            boolean isDuplicate,
            String message,
            ClinicalResult clinicalResult
    ) {}

    private final ClinicalResultRepository results;
    private final MedicalOrderRepository orders;
    private final MedicalEncounterRepository encounters;

    public ClsIntegrationService(ClinicalResultRepository results,
                                 MedicalOrderRepository orders,
                                 MedicalEncounterRepository encounters) {
        this.results = results;
        this.orders = orders;
        this.encounters = encounters;
    }

    @Transactional
    public IngestOutcome ingestResult(ClsEventMessage event, String actor) {
        log.info("Processing CLS event: eventId={}, source={}, resultId={}, version={}",
                event.eventId(), event.sourceSystem(), event.sourceResultId(), event.sourceVersion());

        // 1. Idempotency Check (TC-INT-004, TC-INT-009)
        Optional<ClinicalResult> existing = results.findBySourceSystemAndSourceResultIdAndSourceVersion(
                event.sourceSystem(), event.sourceResultId(), event.sourceVersion());
        if (existing.isPresent()) {
            log.warn("Duplicate CLS event detected for {}:{}:{}, skipping creation.",
                    event.sourceSystem(), event.sourceResultId(), event.sourceVersion());
            return new IngestOutcome(true, existing.get().isQuarantined(), true,
                    "Duplicate event recognized. No business record duplicated.", existing.get());
        }

        // 2. Identity Verification & Mismatch Validation (TC-INT-003)
        boolean mismatch = false;
        String reason = null;

        if (event.orderId() == null) {
            mismatch = true;
            reason = "Missing order ID";
        } else {
            Optional<MedicalOrder> orderOpt = orders.findById(event.orderId());
            if (orderOpt.isEmpty()) {
                mismatch = true;
                reason = "Referenced OrderID not found: " + event.orderId();
            } else {
                MedicalOrder order = orderOpt.get();
                Optional<MedicalEncounter> encOpt = encounters.findById(order.getEncounterId());
                if (encOpt.isEmpty()) {
                    mismatch = true;
                    reason = "Associated Encounter not found for Order: " + order.getOrderId();
                } else {
                    MedicalEncounter encounter = encOpt.get();
                    if (event.patientId() != null && !encounter.getPatientId().equals(event.patientId())) {
                        mismatch = true;
                        reason = String.format("Identity mismatch: Event PatientID %s != Encounter PatientID %s",
                                event.patientId(), encounter.getPatientId());
                    }
                }
            }
        }

        // 3. Quarantine when identity is wrong
        if (mismatch) {
            log.error("CLS result quarantined due to identity/order verification failure: {}", reason);
            // In quarantine scenario, if order is null or invalid, save with quarantine marker
            UUID targetOrderId = event.orderId();
            if (targetOrderId == null || orders.findById(targetOrderId).isEmpty()) {
                // If orderId is invalid or violates FK in DB, we still flag mismatch and do not attach to any patient
                return new IngestOutcome(false, true, false,
                        "Quarantined: " + reason + ". Not attached to any medical record.", null);
            }

            ClinicalResult quarantined = ClinicalResult.create(
                    targetOrderId, event.sourceSystem(), event.sourceResultId(), event.sourceVersion(),
                    event.resultAt(), event.payload(), event.abnormalFlag(),
                    ClinicalResult.Quality.QUARANTINED, actor);
            results.save(quarantined);
            return new IngestOutcome(false, true, false,
                    "Quarantined: " + reason + ". Not attached to any medical record.", quarantined);
        }

        // 4. Valid ingestion
        ClinicalResult validResult = ClinicalResult.create(
                event.orderId(), event.sourceSystem(), event.sourceResultId(), event.sourceVersion(),
                event.resultAt(), event.payload(), event.abnormalFlag(),
                ClinicalResult.Quality.VALID, actor);
        results.save(validResult);

        // Update order status to COMPLETED if applicable
        orders.findById(event.orderId()).ifPresent(order -> {
            if (order.getOrderStatus().allowedNext().contains(MedicalOrder.Status.COMPLETED)) {
                order.changeStatus(MedicalOrder.Status.COMPLETED, actor);
            }
        });

        return new IngestOutcome(true, false, false, "Result successfully ingested", validResult);
    }
}
