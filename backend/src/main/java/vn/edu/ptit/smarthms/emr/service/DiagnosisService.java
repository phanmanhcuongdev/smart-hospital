package vn.edu.ptit.smarthms.emr.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.emr.domain.Diagnosis;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisStatus;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisType;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.port.AuditPort;
import vn.edu.ptit.smarthms.emr.repository.DiagnosisRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;

@Service
public class DiagnosisService {
    private static final String DEV_ACTOR = "dev-stub"; // thay bằng user đăng nhập khi V.2 xong
    private static final Set<EncounterStatus> CAN_ADD =
            Set.of(EncounterStatus.IN_PROGRESS, EncounterStatus.PENDING_LAB_RAD);
    private static final Set<EncounterStatus> CAN_CONFIRM =
            Set.of(EncounterStatus.IN_PROGRESS, EncounterStatus.PENDING_LAB_RAD, EncounterStatus.DIAGNOSED);

    private final DiagnosisRepository diagnoses;
    private final MedicalEncounterRepository encounters;
    private final RecordLockGuard lockGuard;
    private final AuditPort audit;

    public DiagnosisService(DiagnosisRepository diagnoses, MedicalEncounterRepository encounters,
                            RecordLockGuard lockGuard, AuditPort audit) {
        this.diagnoses = diagnoses;
        this.encounters = encounters;
        this.lockGuard = lockGuard;
        this.audit = audit;
    }

    /** Thêm chẩn đoán luôn ở trạng thái PROVISIONAL (giữ nháp), kể cả thiếu ICD-10. */
    @Transactional
    public Diagnosis create(UUID encounterId, DiagnosisType type, String icd10Code, String narrative) {
        MedicalEncounter e = findEncounter(encounterId);
        lockGuard.assertEditable(encounterId);
        if (!CAN_ADD.contains(e.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot add diagnosis when encounter is " + e.getStatus());
        }
        return diagnoses.save(Diagnosis.create(encounterId, type, icd10Code, narrative, DEV_ACTOR));
    }

    @Transactional(readOnly = true)
    public List<Diagnosis> list(UUID encounterId) {
        findEncounter(encounterId);
        return diagnoses.findByEncounterIdOrderByCreatedAtAsc(encounterId);
    }

    /** IV7 M-03 / AC-EMR-01.3: chẩn đoán chính cần ICD-10 và mỗi lần khám chỉ có 1 chẩn đoán chính đã xác nhận. */
    @Transactional
    public Diagnosis confirm(UUID encounterId, UUID diagnosisId) {
        MedicalEncounter e = findEncounter(encounterId);
        lockGuard.assertEditable(encounterId);
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
        if (d.getDiagnosisType() == DiagnosisType.PRIMARY) {
            if (d.getIcd10Code() == null || d.getIcd10Code().isBlank()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "ICD-10 code is required to confirm a primary diagnosis");
            }
            if (diagnoses.existsByEncounterIdAndDiagnosisTypeAndDiagnosisStatus(
                    encounterId, DiagnosisType.PRIMARY, DiagnosisStatus.CONFIRMED)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Encounter already has a confirmed primary diagnosis");
            }
        }
        // Tạm dùng bác sĩ phụ trách làm người xác nhận; V.2 thay bằng người dùng đăng nhập.
        d.confirm(e.getAttendingEmployeeId(), DEV_ACTOR);
        audit.success("DIAGNOSIS_CONFIRMED", "DIAGNOSIS", d.getDiagnosisId(), null);
        return d;
    }

    private MedicalEncounter findEncounter(UUID id) {
        return encounters.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
    }
}