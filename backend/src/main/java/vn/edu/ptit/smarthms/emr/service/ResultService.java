package vn.edu.ptit.smarthms.emr.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalOrder;
import vn.edu.ptit.smarthms.emr.repository.ClinicalResultRepository;
import vn.edu.ptit.smarthms.emr.repository.ClinicalResultRepository.ResultRow;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalOrderRepository;

/** IF-EMR-04 / IV7 M-07: nhận kết quả CLS giả lập, cách ly khi sai bệnh nhân, không ghi đè khi trùng. */
@Service
public class ResultService {
    public enum Disposition { ACCEPTED, QUARANTINED, DUPLICATE }

    public record Outcome(Disposition disposition, ResultRow result) {}

    private static final String ACTOR = "cls-mock";
    private static final Set<String> TRUSTED_SOURCES = Set.of("CLS_MOCK");
    private static final Set<MedicalOrder.Status> ACCEPTING =
            Set.of(MedicalOrder.Status.ACKNOWLEDGED, MedicalOrder.Status.IN_PROGRESS);

    private final ClinicalResultRepository results;
    private final MedicalOrderRepository orders;
    private final MedicalEncounterRepository encounters;

    public ResultService(ClinicalResultRepository results, MedicalOrderRepository orders,
                         MedicalEncounterRepository encounters) {
        this.results = results;
        this.orders = orders;
        this.encounters = encounters;
    }

    public Outcome accept(UUID orderId, String sourceSystem, String sourceResultId, String sourceVersion,
                          UUID patientId, OffsetDateTime resultAt, String payload, Boolean abnormalFlag) {
        String system = sourceSystem.trim();
        String resultKey = sourceResultId.trim();
        String version = sourceVersion.trim();
        if (!TRUSTED_SOURCES.contains(system)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Untrusted result source");
        }
        MedicalOrder order = orders.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        MedicalEncounter encounter = encounters.findById(order.getEncounterId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
        if (!ACCEPTING.contains(order.getOrderStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order does not accept results when it is " + order.getOrderStatus());
        }
        if (!results.isValidJson(payload)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payload must be valid JSON");
        }
        String quality = encounter.getPatientId().equals(patientId) ? "VALID" : "QUARANTINED";
        UUID id = UUID.randomUUID();
        boolean inserted = results.insertIfAbsent(id, orderId, system, resultKey, version, resultAt,
                payload, abnormalFlag, quality, ACTOR);
        if (!inserted) {
            ResultRow existing = results.findBySource(system, resultKey, version).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.CONFLICT, "Result conflict, please retry"));
            if (!existing.orderId().equals(orderId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Source result id is already used for another order");
            }
            return new Outcome(Disposition.DUPLICATE, existing);
        }
        ResultRow saved = results.findById(id).orElseThrow(() ->
                new IllegalStateException("Result row missing after insert"));
        return new Outcome("VALID".equals(quality) ? Disposition.ACCEPTED : Disposition.QUARANTINED, saved);
    }

    public List<ResultRow> list(UUID orderId) {
        orders.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return results.listByOrder(orderId);
    }
}