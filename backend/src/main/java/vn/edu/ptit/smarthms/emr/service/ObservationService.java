package vn.edu.ptit.smarthms.emr.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.domain.ClinicalObservation;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.repository.ClinicalObservationRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;

@Service
public class ObservationService {
    /** Giả định, cần đối chiếu BR trong III.3/IV.7. */
    private static final Set<EncounterStatus> CAN_RECORD = Set.of(
            EncounterStatus.WAITING, EncounterStatus.IN_PROGRESS, EncounterStatus.PENDING_LAB_RAD);

    private final ClinicalObservationRepository observations;
    private final MedicalEncounterRepository encounters;

    public ObservationService(ClinicalObservationRepository observations,
                              MedicalEncounterRepository encounters) {
        this.observations = observations;
        this.encounters = encounters;
    }

    @Transactional
    public ClinicalObservation create(Actor actor, UUID encounterId, String metricCode, BigDecimal numericValue,
            String textValue, String unitCode, OffsetDateTime observedAt,
            ClinicalObservation.SourceType sourceType, String sourceRef,
            ClinicalObservation.Quality quality) {
        actor.requireAnyRole("DOCTOR", "NURSE", "ADMIN");
        MedicalEncounter e = findEncounter(encounterId);
        if (!CAN_RECORD.contains(e.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot record observation when encounter is " + e.getStatus());
        }
        boolean hasNumber = numericValue != null;
        boolean hasText = textValue != null && !textValue.isBlank();
        if (hasNumber == hasText) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide exactly one of numericValue or textValue");
        }
        return observations.save(ClinicalObservation.create(
                encounterId, metricCode.trim(), numericValue, hasText ? textValue : null, unitCode,
                observedAt != null ? observedAt : OffsetDateTime.now(),
                sourceType != null ? sourceType : ClinicalObservation.SourceType.MANUAL,
                sourceRef != null && !sourceRef.isBlank() ? sourceRef : "manual-entry",
                quality != null ? quality : ClinicalObservation.Quality.VALID,
                actor.principalName()));
    }

    @Transactional(readOnly = true)
    public List<ClinicalObservation> list(UUID encounterId) {
        findEncounter(encounterId);
        return observations.findByEncounterIdOrderByObservedAtAsc(encounterId);
    }

    private MedicalEncounter findEncounter(UUID id) {
        return encounters.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
    }
}
