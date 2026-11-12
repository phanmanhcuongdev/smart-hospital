package vn.edu.ptit.smarthms.emr.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.domain.Diagnosis;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisStatus;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisType;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.repository.DiagnosisRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;

@Service
public class DiagnosisService {
    /** Giả định, cần đối chiếu BR trong III.3/IV.7. */
    private static final Set<EncounterStatus> CAN_ADD =
            Set.of(EncounterStatus.IN_PROGRESS, EncounterStatus.PENDING_LAB_RAD);
    private static final Set<EncounterStatus> CAN_CONFIRM =
            Set.of(EncounterStatus.IN_PROGRESS, EncounterStatus.PENDING_LAB_RAD, EncounterStatus.DIAGNOSED);

    private final DiagnosisRepository diagnoses;
    private final MedicalEncounterRepository encounters;
    private final ReferenceLookupPort lookup;

    @Autowired
    public DiagnosisService(DiagnosisRepository diagnoses, MedicalEncounterRepository encounters,
                            ReferenceLookupPort lookup) {
        this.diagnoses = diagnoses;
        this.encounters = encounters;
        this.lookup = lookup;
    }

    public DiagnosisService(DiagnosisRepository diagnoses, MedicalEncounterRepository encounters) {
        this(diagnoses, encounters, null);
    }

    @Transactional
    public Diagnosis create(Actor actor, UUID encounterId, DiagnosisType type, String icd10Code, String narrative) {
        actor.requireAnyRole("DOCTOR", "ADMIN");
        MedicalEncounter e = findEncounter(encounterId);
        if (!CAN_ADD.contains(e.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot add diagnosis when encounter is " + e.getStatus());
        }

        if (lookup != null && e.getAttendingEmployeeId() != null) {
            var practitionerOpt = lookup.getPractitionerDetails(e.getAttendingEmployeeId());
            if (practitionerOpt.isPresent()) {
                var p = practitionerOpt.get();
                if (!p.active()) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Attending practitioner is not in ACTIVE status (current: " + p.status() + ")");
                }
                if (!p.hasVerifiedLicense()) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Attending practitioner does not hold a valid verified medical practice license (CCHN)");
                }
            }
        }

        if (type == DiagnosisType.PRIMARY
                && diagnoses.existsByEncounterIdAndDiagnosisType(encounterId, DiagnosisType.PRIMARY)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Encounter already has a primary diagnosis");
        }
        return diagnoses.save(Diagnosis.create(encounterId, type, icd10Code, narrative, actor.principalName()));
    }

    @Transactional(readOnly = true)
    public List<Diagnosis> list(UUID encounterId) {
        findEncounter(encounterId);
        return diagnoses.findByEncounterIdOrderByCreatedAtAsc(encounterId);
    }

    @Transactional
    public Diagnosis confirm(Actor actor, UUID encounterId, UUID diagnosisId) {
        actor.requireAnyRole("DOCTOR", "ADMIN");
        MedicalEncounter e = findEncounter(encounterId);
        Diagnosis d = diagnoses.findById(diagnosisId)
                .filter(x -> x.getEncounterId().equals(encounterId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diagnosis not found"));
        if (!CAN_CONFIRM.contains(e.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot confirm diagnosis when encounter is " + e.getStatus());
        }
        if (d.getDiagnosisStatus() == DiagnosisStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Diagnosis already confirmed");
        }
        d.confirm(e.getAttendingEmployeeId(), actor.principalName());
        return d;
    }

    private MedicalEncounter findEncounter(UUID id) {
        return encounters.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
    }
}
