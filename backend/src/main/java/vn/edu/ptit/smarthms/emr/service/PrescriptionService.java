package vn.edu.ptit.smarthms.emr.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.common.error.ConflictException;
import vn.edu.ptit.smarthms.common.error.ForbiddenException;
import vn.edu.ptit.smarthms.common.error.NotFoundException;
import vn.edu.ptit.smarthms.common.error.ValidationException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.api.EmrRequests.PrescriptionItemRequest;
import vn.edu.ptit.smarthms.emr.api.EmrRequests.PrescriptionRequest;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.PrescriptionItemResponse;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.PrescriptionResponse;
import vn.edu.ptit.smarthms.emr.domain.AllergyFixture;
import vn.edu.ptit.smarthms.emr.domain.AllergyFixtureRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecordRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicationFixture;
import vn.edu.ptit.smarthms.emr.domain.MedicationFixtureRepository;
import vn.edu.ptit.smarthms.emr.domain.Prescription;
import vn.edu.ptit.smarthms.emr.domain.PrescriptionItem;
import vn.edu.ptit.smarthms.emr.domain.PrescriptionItemRepository;
import vn.edu.ptit.smarthms.emr.domain.PrescriptionRepository;

/** Prescriptions (FR-EMR-03, BR-053 to BR-055). Prescribed is not dispensed. */
@Service
@Transactional
public class PrescriptionService {

    private final MedicalEncounterRepository encounters;
    private final PrescriptionRepository prescriptions;
    private final PrescriptionItemRepository items;
    private final MedicationFixtureRepository medications;
    private final AllergyFixtureRepository allergies;
    private final MedicalRecordRepository records;
    private final PractitionerPort practitionerPort;

    public PrescriptionService(MedicalEncounterRepository encounters,
            PrescriptionRepository prescriptions, PrescriptionItemRepository items,
            MedicationFixtureRepository medications, AllergyFixtureRepository allergies,
            MedicalRecordRepository records, PractitionerPort practitionerPort) {
        this.encounters = encounters;
        this.prescriptions = prescriptions;
        this.items = items;
        this.medications = medications;
        this.allergies = allergies;
        this.records = records;
        this.practitionerPort = practitionerPort;
    }

    public PrescriptionResponse createPrescription(Actor actor, UUID encounterId,
            PrescriptionRequest request) {
        actor.requireRole("DOCTOR");
        MedicalEncounter encounter = requireEncounter(encounterId);
        UUID doctorId = requireAttending(actor, encounter);
        requirePrescribingDoctor(doctorId);
        requireActive(encounter);
        requireRecordNotLocked(encounterId);
        List<String> warnings = new ArrayList<>();
        List<AllergyFixture> patientAllergies = allergies.findByPatientId(encounter.getPatientId());
        List<PrescriptionItemRequest> checked = new ArrayList<>();
        List<MedicationFixture> drugs = new ArrayList<>();
        for (PrescriptionItemRequest item : request.items()) {
            MedicationFixture drug = medications.findById(item.medicationId())
                    .orElseThrow(() -> new NotFoundException("Medication not found: " + item.medicationId()));
            if (drug.isActive() == false) {
                throw new ValidationException("Medication is not active: " + drug.getDisplayName());
            }
            AllergyFixture match = null;
            for (AllergyFixture allergy : patientAllergies) {
                if (allergy.getAllergen() != null
                        && allergy.getAllergen().equalsIgnoreCase(drug.getDisplayName())) {
                    match = allergy;
                }
            }
            if (match != null && match.getStatus() == AllergyFixture.Status.HAS_ALLERGY) {
                throw new ValidationException("Allergy recorded for " + drug.getDisplayName()
                        + " - prescription blocked.");
            }
            if (match != null && match.getStatus() == AllergyFixture.Status.UNKNOWN) {
                warnings.add("Allergy status UNKNOWN for " + drug.getDisplayName()
                        + " - not declared safe.");
            }
            if (match == null) {
                warnings.add("No allergy information on file for " + drug.getDisplayName()
                        + " - not declared safe.");
            }
            checked.add(item);
            drugs.add(drug);
        }
        Prescription prescription = new Prescription(encounterId, doctorId);
        prescription.setCreatedBy(actor.principalName());
        prescription.setUpdatedBy(actor.principalName());
        Prescription saved = prescriptions.save(prescription);
        List<PrescriptionItemResponse> itemResponses = new ArrayList<>();
        int lineNo = 1;
        for (int index = 0; index < checked.size(); index++) {
            PrescriptionItemRequest item = checked.get(index);
            PrescriptionItem entity = new PrescriptionItem(saved.getId(), lineNo,
                    item.medicationId(), drugs.get(index).getDisplayName(), item.dose(), item.route(),
                    item.frequency(), item.daysCount(), item.quantity(), item.unitCode());
            entity.setCreatedBy(actor.principalName());
            entity.setUpdatedBy(actor.principalName());
            itemResponses.add(PrescriptionItemResponse.from(items.save(entity)));
            lineNo = lineNo + 1;
        }
        return toResponse(saved, itemResponses, warnings);
    }

    public PrescriptionResponse issuePrescription(Actor actor, UUID prescriptionId) {
        actor.requireRole("DOCTOR");
        Prescription prescription = requirePrescription(prescriptionId);
        requirePrescribingDoctor(prescription.getPrescriberId());
        if (prescription.getStatus() != Prescription.Status.DRAFT) {
            throw new ConflictException("Only a DRAFT prescription can be issued.");
        }
        prescription.setStatus(Prescription.Status.ORDERED);
        prescription.setIssuedAt(Instant.now());
        prescription.setUpdatedBy(actor.principalName());
        Prescription saved = prescriptions.save(prescription);
        return toResponse(saved, loadItems(saved.getId()), List.of());
    }

    public PrescriptionResponse cancelPrescription(Actor actor, UUID prescriptionId) {
        actor.requireRole("DOCTOR");
        Prescription prescription = requirePrescription(prescriptionId);
        requirePrescribingDoctor(prescription.getPrescriberId());
        if (prescription.getStatus() == Prescription.Status.CANCELLED) {
            throw new ConflictException("Prescription is already cancelled.");
        }
        if (prescription.getStatus() == Prescription.Status.ORDERED) {
            throw new ConflictException("An issued prescription cannot be cancelled; issue a replacement instead.");
        }
        prescription.setStatus(Prescription.Status.CANCELLED);
        prescription.setUpdatedBy(actor.principalName());
        Prescription saved = prescriptions.save(prescription);
        return toResponse(saved, loadItems(saved.getId()), List.of());
    }

    public PrescriptionResponse getPrescription(Actor actor, UUID prescriptionId) {
        actor.requireAnyRole("DOCTOR", "NURSE", "CLS_TECHNICIAN", "RECORDS_MANAGER", "RECEPTION");
        Prescription prescription = requirePrescription(prescriptionId);
        return toResponse(prescription, loadItems(prescription.getId()), List.of());
    }

    public List<PrescriptionResponse> listByEncounter(Actor actor, UUID encounterId) {
        actor.requireAnyRole("DOCTOR", "NURSE", "CLS_TECHNICIAN", "RECORDS_MANAGER", "RECEPTION");
        MedicalEncounter encounter = requireEncounter(encounterId);
        requireScope(actor, encounter);
        List<PrescriptionResponse> result = new ArrayList<>();
        for (Prescription prescription : prescriptions.findByEncounterId(encounter.getId())) {
            result.add(toResponse(prescription, loadItems(prescription.getId()), List.of()));
        }
        return result;
    }

    private List<PrescriptionItemResponse> loadItems(UUID prescriptionId) {
        List<PrescriptionItemResponse> result = new ArrayList<>();
        for (PrescriptionItem item : items.findByIdPrescriptionId(prescriptionId)) {
            result.add(PrescriptionItemResponse.from(item));
        }
        return result;
    }

    private PrescriptionResponse toResponse(Prescription prescription,
            List<PrescriptionItemResponse> itemResponses, List<String> warnings) {
        return new PrescriptionResponse(prescription.getId(), prescription.getEncounterId(),
                prescription.getPrescriberId(), prescription.getStatus().name(), prescription.getIssuedAt(),
                itemResponses, warnings, prescription.getCreatedAt(), prescription.getCreatedBy());
    }

    private MedicalEncounter requireEncounter(UUID encounterId) {
        return encounters.findById(encounterId)
                .orElseThrow(() -> new NotFoundException("Encounter not found: " + encounterId));
    }

    private Prescription requirePrescription(UUID prescriptionId) {
        return prescriptions.findById(prescriptionId)
                .orElseThrow(() -> new NotFoundException("Prescription not found: " + prescriptionId));
    }

    private UUID requireAttending(Actor actor, MedicalEncounter encounter) {
        UUID doctorId = practitionerPort.employeeIdForCode(actor.principalName()).orElse(null);
        if (doctorId == null || doctorId.equals(encounter.getAttendingEmployeeId()) == false) {
            throw new ForbiddenException("Only the attending doctor may perform this action.");
        }
        return doctorId;
    }

    private void requirePrescribingDoctor(UUID doctorId) {
        if (practitionerPort.isActiveEmployee(doctorId) == false) {
            throw new ForbiddenException("Prescriber is not an ACTIVE employee.");
        }
        if (practitionerPort.hasValidCchn(doctorId, LocalDate.now()) == false) {
            throw new ForbiddenException("Prescriber must be an ACTIVE doctor with a valid CCHN.");
        }
    }

    private void requireActive(MedicalEncounter encounter) {
        if (encounter.getStatus() == MedicalEncounter.Status.COMPLETED
                || encounter.getStatus() == MedicalEncounter.Status.CANCELLED
                || encounter.getStatus() == MedicalEncounter.Status.NO_SHOW) {
            throw new ConflictException("Encounter is closed.");
        }
    }

    private void requireRecordNotLocked(UUID encounterId) {
        records.findByEncounterId(encounterId).ifPresent(record -> {
            if (record.getStatus() == MedicalRecord.Status.LOCKED
                    || record.getStatus() == MedicalRecord.Status.ARCHIVED) {
                throw new ConflictException("Medical record is locked.");
            }
        });
    }

    private void requireScope(Actor actor, MedicalEncounter encounter) {
        if (actor.hasRole("DOCTOR")) {
            UUID doctorId = practitionerPort.employeeIdForCode(actor.principalName()).orElse(null);
            if (doctorId == null || doctorId.equals(encounter.getAttendingEmployeeId()) == false) {
                throw new ForbiddenException("Doctor may only view assigned encounters.");
            }
        }
    }
}
