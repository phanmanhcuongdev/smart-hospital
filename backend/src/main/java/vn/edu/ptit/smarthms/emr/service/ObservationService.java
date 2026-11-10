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
import vn.edu.ptit.smarthms.emr.domain.ClinicalObservation;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.repository.ClinicalObservationRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;

@Service
public class ObservationService {
    private static final String DEV_ACTOR = "dev-stub"; // thay bằng user đăng nhập khi V.2 xong
    /** IV7 M-02: chỉ ghi khi lần khám đang IN_PROGRESS. */
    private static final Set<EncounterStatus> CAN_RECORD = Set.of(EncounterStatus.IN_PROGRESS);

    private final ClinicalObservationRepository observations;
    private final MedicalEncounterRepository encounters;

    public ObservationService(ClinicalObservationRepository observations,
                              MedicalEncounterRepository encounters) {
        this.observations = observations;
        this.encounters = encounters;
    }

    @Transactional
    public ClinicalObservation create(UUID encounterId, String metricCode, BigDecimal numericValue,
            String textValue, String unitCode, OffsetDateTime observedAt,
            ClinicalObservation.SourceType sourceType, String sourceRef,
            ClinicalObservation.Quality quality) {
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
        String unit = unitCode == null || unitCode.isBlank() ? null : unitCode.trim();
        if (hasNumber && unit == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "unitCode is required for a numeric observation");
        }
        OffsetDateTime when = observedAt != null ? observedAt : OffsetDateTime.now();
        if (when.isAfter(OffsetDateTime.now().plusMinutes(5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "observedAt cannot be in the future");
        }
        return observations.save(ClinicalObservation.create(
                encounterId, metricCode.trim(), numericValue, hasText ? textValue : null, unit, when,
                sourceType != null ? sourceType : ClinicalObservation.SourceType.MANUAL,
                sourceRef != null && !sourceRef.isBlank() ? sourceRef : "manual-entry",
                quality != null ? quality : ClinicalObservation.Quality.VALID,
                DEV_ACTOR));
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