package vn.edu.ptit.smarthms.emr.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.common.error.ConflictException;
import vn.edu.ptit.smarthms.common.error.ForbiddenException;
import vn.edu.ptit.smarthms.common.error.NotFoundException;
import vn.edu.ptit.smarthms.common.error.ValidationException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.api.EmrRequests.DiagnosisRequest;
import vn.edu.ptit.smarthms.emr.api.EmrRequests.OpenEncounterRequest;
import vn.edu.ptit.smarthms.emr.api.EmrRequests.VitalsRequest;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.DiagnosisResponse;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.EncounterDetailResponse;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.EncounterResponse;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.ObservationResponse;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.OrderResponse;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.PrescriptionItemResponse;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.PrescriptionResponse;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.RecordResponse;
import vn.edu.ptit.smarthms.emr.domain.ClinicalObservation;
import vn.edu.ptit.smarthms.emr.domain.ClinicalObservationRepository;
import vn.edu.ptit.smarthms.emr.domain.Diagnosis;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalOrderRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecordRepository;
import vn.edu.ptit.smarthms.emr.domain.PatientFixture;
import vn.edu.ptit.smarthms.emr.domain.PatientFixtureRepository;
import vn.edu.ptit.smarthms.emr.domain.PrescriptionRepository;

/** Encounters, vitals and diagnoses (FR-EMR-01). */
@Service
@Transactional
public class EncounterService {

    private final MedicalEncounterRepository encounters;
    private final ClinicalObservationRepository observations;
    private final DiagnosisRepository diagnoses;
    private final MedicalRecordRepository records;
    private final MedicalOrderRepository orders;
    private final PrescriptionRepository prescriptions;
    private final PatientFixtureRepository patients;
    private final PractitionerPort practitionerPort;

    public EncounterService(MedicalEncounterRepository encounters,
            ClinicalObservationRepository observations, DiagnosisRepository diagnoses,
            MedicalRecordRepository records, MedicalOrderRepository orders,
            PrescriptionRepository prescriptions, PatientFixtureRepository patients,
            PractitionerPort practitionerPort) {
        this.encounters = encounters;
        this.observations = observations;
        this.diagnoses = diagnoses;
        this.records = records;
        this.orders = orders;
        this.prescriptions = prescriptions;
        this.patients = patients;
        this.practitionerPort = practitionerPort;
    }

    public EncounterResponse openEncounter(Actor actor, OpenEncounterRequest request) {
        actor.requireAnyRole("RECEPTION", "DOCTOR");
        PatientFixture patient = patients.findById(request.patientId())
                .orElseThrow(() -> NotFoundException.forId("Patient", request.patientId()));
        if (patient.getIdentityStatus() == PatientFixture.IdentityStatus.UNVERIFIED) {
            throw new ValidationException("Patient identity is not verified.");
        }
        if (patient.getIdentityStatus() == PatientFixture.IdentityStatus.FLAGGED) {
            throw new ForbiddenException("Patient identity is flagged for verification.");
        }
        if (practitionerPort.isActiveEmployee(request.attendingEmployeeId()) == false) {
            throw new ForbiddenException("Attending employee is not an ACTIVE employee.");
        }
        MedicalEncounter encounter = new MedicalEncounter(request.patientId(),
                request.appointmentId(), request.attendingEmployeeId());
        encounter.setCreatedBy(actor.principalName());
        encounter.setUpdatedBy(actor.principalName());
        return EncounterResponse.from(encounters.save(encounter));
    }

    public EncounterResponse startEncounter(Actor actor, UUID encounterId) {
        MedicalEncounter encounter = requireAttendingDoctor(actor, encounterId);
        if (encounter.getStatus() != MedicalEncounter.Status.WAITING) {
            throw new ConflictException("Only a WAITING encounter can be started.");
        }
        encounter.setStatus(MedicalEncounter.Status.IN_PROGRESS);
        encounter.setStartedAt(Instant.now());
        encounter.setUpdatedBy(actor.principalName());
        encounters.save(encounter);
        if (records.findByEncounterId(encounterId).isEmpty()) {
            MedicalRecord record = new MedicalRecord(encounterId);
            record.setCreatedBy(actor.principalName());
            record.setUpdatedBy(actor.principalName());
            records.save(record);
        }
        return EncounterResponse.from(encounter);
    }

    public EncounterResponse completeEncounter(Actor actor, UUID encounterId) {
        MedicalEncounter encounter = requireAttendingDoctor(actor, encounterId);
        if (encounter.getStatus() != MedicalEncounter.Status.IN_PROGRESS
                && encounter.getStatus() != MedicalEncounter.Status.DIAGNOSED
                && encounter.getStatus() != MedicalEncounter.Status.PENDING_LAB_RAD) {
            throw new ConflictException("Only an in-progress encounter can be completed.");
        }
        encounter.setStatus(MedicalEncounter.Status.COMPLETED);
        encounter.setCompletedAt(Instant.now());
        encounter.setUpdatedBy(actor.principalName());
        return EncounterResponse.from(encounters.save(encounter));
    }

    public EncounterResponse cancelEncounter(Actor actor, UUID encounterId) {
        actor.requireAnyRole("RECEPTION", "DOCTOR");
        MedicalEncounter encounter = requireEncounter(encounterId);
        if (actor.hasRole("DOCTOR")) {
            requireAttending(actor, encounter);
        }
        if (encounter.getStatus() == MedicalEncounter.Status.COMPLETED
                || encounter.getStatus() == MedicalEncounter.Status.CANCELLED
                || encounter.getStatus() == MedicalEncounter.Status.NO_SHOW) {
            throw new ConflictException("Encounter is already closed.");
        }
        encounter.setStatus(MedicalEncounter.Status.CANCELLED);
        encounter.setUpdatedBy(actor.principalName());
        return EncounterResponse.from(encounters.save(encounter));
    }

    public EncounterResponse getEncounter(Actor actor, UUID encounterId) {
        MedicalEncounter encounter = requireEncounter(encounterId);
        requireVisible(actor, encounter);
        return EncounterResponse.from(encounter);
    }

    public List<EncounterResponse> listEncounters(Actor actor) {
        actor.requireAnyRole("DOCTOR", "NURSE", "CLS_TECHNICIAN", "RECORDS_MANAGER", "RECEPTION");
        if (actor.hasRole("DOCTOR") && actor.hasAnyRole("RECORDS_MANAGER", "RECEPTION") == false) {
            UUID doctorId = resolveDoctorId(actor);
            return encounters.findByAttendingEmployeeId(doctorId).stream()
                    .map(EncounterResponse::from).toList();
        }
        return encounters.findAll().stream().map(EncounterResponse::from).toList();
    }

    public ObservationResponse recordVitals(Actor actor, UUID encounterId, VitalsRequest request) {
        actor.requireRole("NURSE");
        MedicalEncounter encounter = requireEncounter(encounterId);
        requireActive(encounter);
        requireRecordNotLocked(encounterId);
        if ((request.numericValue() == null) == (request.textValue() == null)) {
            throw new ValidationException("Exactly one of numericValue and textValue must be given.");
        }
        if (request.textValue() != null && request.textValue().isBlank()) {
            throw new ValidationException("textValue must not be blank.");
        }
        ClinicalObservation observation = new ClinicalObservation(encounterId, request.metricCode(),
                request.numericValue(), request.textValue(), request.unitCode(), request.observedAt(),
                ClinicalObservation.SourceType.MANUAL);
        observation.setCreatedBy(actor.principalName());
        observation.setUpdatedBy(actor.principalName());
        return ObservationResponse.from(observations.save(observation));
    }

    public List<ObservationResponse> listVitals(Actor actor, UUID encounterId) {
        MedicalEncounter encounter = requireEncounter(encounterId);
        requireVisible(actor, encounter);
        return observations.findByEncounterId(encounterId).stream().map(ObservationResponse::from).toList();
    }

    public DiagnosisResponse recordDiagnosis(Actor actor, UUID encounterId, DiagnosisRequest request) {
        MedicalEncounter encounter = requireAttendingDoctor(actor, encounterId);
        requireActive(encounter);
        requireRecordNotLocked(encounterId);
        Diagnosis.Type type;
        Diagnosis.Status status;
        try {
            type = Diagnosis.Type.valueOf(request.type());
            status = Diagnosis.Status.valueOf(request.status());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Unknown diagnosis type or status.");
        }
        if (status == Diagnosis.Status.CONFIRMED) {
            if (request.icd10Code() == null || request.icd10Code().isBlank()) {
                throw new ValidationException("A confirmed diagnosis needs a primary ICD-10 code.");
            }
            if (type == Diagnosis.Type.PRIMARY
                    && diagnoses.findByEncounterIdAndType(encounterId, Diagnosis.Type.PRIMARY)
                            .filter(d -> d.getStatus() == Diagnosis.Status.CONFIRMED).isPresent()) {
                throw new ConflictException("Encounter already has a confirmed primary diagnosis.");
            }
        }
        Diagnosis diagnosis = new Diagnosis(encounterId, type, request.icd10Code(), request.narrative());
        diagnosis.setStatus(status);
        if (status == Diagnosis.Status.CONFIRMED) {
            diagnosis.setConfirmedBy(resolveDoctorId(actor));
        }
        diagnosis.setCreatedBy(actor.principalName());
        diagnosis.setUpdatedBy(actor.principalName());
        Diagnosis saved = diagnoses.save(diagnosis);
        if (encounter.getStatus() == MedicalEncounter.Status.IN_PROGRESS
                || encounter.getStatus() == MedicalEncounter.Status.PENDING_LAB_RAD) {
            encounter.setStatus(MedicalEncounter.Status.DIAGNOSED);
            encounter.setUpdatedBy(actor.principalName());
            encounters.save(encounter);
        }
        return DiagnosisResponse.from(saved);
    }

    public List<DiagnosisResponse> listDiagnoses(Actor actor, UUID encounterId) {
        MedicalEncounter encounter = requireEncounter(encounterId);
        requireVisible(actor, encounter);
        return diagnoses.findByEncounterId(encounterId).stream().map(DiagnosisResponse::from).toList();
    }

    public EncounterDetailResponse getDetail(Actor actor, UUID encounterId) {
        MedicalEncounter encounter = requireEncounter(encounterId);
        requireVisible(actor, encounter);
        List<ObservationResponse> vitals = observations.findByEncounterId(encounterId).stream()
                .map(ObservationResponse::from).toList();
        List<DiagnosisResponse> diagnosisList = diagnoses.findByEncounterId(encounterId).stream()
                .map(DiagnosisResponse::from).toList();
        List<OrderResponse> orderList = orders.findByEncounterId(encounterId).stream()
                .map(OrderResponse::from).toList();
        List<PrescriptionResponse> prescriptionList = prescriptions.findByEncounterId(encounterId).stream()
                .map(p -> new PrescriptionResponse(p.getId(), p.getEncounterId(), p.getPrescriberId(),
                        p.getStatus().name(), p.getIssuedAt(), List.of(), List.of(),
                        p.getCreatedAt(), p.getCreatedBy())).toList();
        RecordResponse record = records.findByEncounterId(encounterId).map(RecordResponse::from).orElse(null);
        return new EncounterDetailResponse(EncounterResponse.from(encounter), vitals, diagnosisList,
                orderList, prescriptionList, record);
    }

    public MedicalEncounter requireEncounter(UUID encounterId) {
        return encounters.findById(encounterId)
                .orElseThrow(() -> NotFoundException.forId("Encounter", encounterId));
    }

    public UUID resolveDoctorId(Actor actor) {
        return practitionerPort.employeeIdForCode(actor.principalName())
                .orElseThrow(() -> new ForbiddenException("Caller has no linked employee record."));
    }

    private MedicalEncounter requireAttendingDoctor(Actor actor, UUID encounterId) {
        actor.requireRole("DOCTOR");
        MedicalEncounter encounter = requireEncounter(encounterId);
        requireAttending(actor, encounter);
        return encounter;
    }

    private void requireAttending(Actor actor, MedicalEncounter encounter) {
        UUID doctorId = resolveDoctorId(actor);
        if (encounter.getAttendingEmployeeId().equals(doctorId) == false) {
            throw new ForbiddenException("Encounter is assigned to another doctor.");
        }
    }

    private void requireVisible(Actor actor, MedicalEncounter encounter) {
        if (actor.hasRole("DOCTOR")
                && actor.hasAnyRole("RECORDS_MANAGER", "RECEPTION", "NURSE", "CLS_TECHNICIAN") == false) {
            requireAttending(actor, encounter);
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
