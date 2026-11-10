package vn.edu.ptit.smarthms.emr.service;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.emr.domain.Addendum;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisStatus;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisType;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;
import vn.edu.ptit.smarthms.emr.domain.RecordStatus;
import vn.edu.ptit.smarthms.emr.port.AuditPort;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.repository.AddendumRepository;
import vn.edu.ptit.smarthms.emr.repository.DiagnosisRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalRecordRepository;
import vn.edu.ptit.smarthms.emr.repository.RecordSnapshotRepository;
import vn.edu.ptit.smarthms.emr.repository.RecordSnapshotRepository.VersionRow;

@Service
public class RecordService {
    private static final String DEV_ACTOR = "dev-stub"; // thay bằng user đăng nhập khi V.2 xong
    private static final String TARGET = "MEDICAL_RECORD";

    private final MedicalRecordRepository records;
    private final MedicalEncounterRepository encounters;
    private final DiagnosisRepository diagnoses;
    private final AddendumRepository addenda;
    private final RecordSnapshotRepository snapshots;
    private final ReferenceLookupPort lookup;
    private final AuditPort audit;

    public RecordService(MedicalRecordRepository records, MedicalEncounterRepository encounters,
                         DiagnosisRepository diagnoses, AddendumRepository addenda,
                         RecordSnapshotRepository snapshots, ReferenceLookupPort lookup, AuditPort audit) {
        this.records = records;
        this.encounters = encounters;
        this.diagnoses = diagnoses;
        this.addenda = addenda;
        this.snapshots = snapshots;
        this.lookup = lookup;
        this.audit = audit;
    }

    /** Đọc hồ sơ. Hồ sơ đã khóa hoặc lưu trữ thì mỗi lần xem đều được ghi audit (BR-045). */
    @Transactional
    public MedicalRecord metadata(UUID encounterId) {
        MedicalRecord r = find(encounterId);
        auditLockedRead(r);
        return r;
    }

    /** Chốt một phiên bản hồ sơ đang soạn thảo. */
    @Transactional
    public VersionRow snapshot(UUID encounterId, int expectedVersion) {
        MedicalRecord r = lockAndCheck(encounterId, expectedVersion);
        requireStatus(r, RecordStatus.DRAFT, "create a version");
        int versionNo = r.getCurrentVersion();
        snapshots.insertVersion(r.getRecordId(), versionNo, snapshots.buildPayload(encounterId), DEV_ACTOR);
        r.advanceVersion(DEV_ACTOR);
        return snapshots.get(r.getRecordId(), versionNo).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<VersionRow> listVersions(UUID encounterId) {
        return snapshots.list(find(encounterId).getRecordId());
    }

    @Transactional
    public VersionRow getVersion(UUID encounterId, int versionNo) {
        MedicalRecord r = find(encounterId);
        VersionRow row = snapshots.get(r.getRecordId(), versionNo).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Record version not found"));
        auditLockedRead(r);
        return row;
    }

    @Transactional
    public MedicalRecord submit(UUID encounterId, int expectedVersion) {
        MedicalRecord r = lockAndCheck(encounterId, expectedVersion);
        requireStatus(r, RecordStatus.DRAFT, "submit the record");
        requireChecklist(encounterId);
        r.submitForSignature(DEV_ACTOR);
        return r;
    }

    /** IV7-SM-02: PENDING_SIGNATURE -> DRAFT (trả về sửa). */
    @Transactional
    public MedicalRecord returnForCorrection(UUID encounterId, int expectedVersion) {
        MedicalRecord r = lockAndCheck(encounterId, expectedVersion);
        requireStatus(r, RecordStatus.PENDING_SIGNATURE, "return the record for correction");
        r.returnToDraft(DEV_ACTOR);
        return r;
    }

    /** Khóa demo (IV7 M-13 / TX-04). Không phải chữ ký số pháp lý. */
    @Transactional
    public MedicalRecord lock(UUID encounterId, UUID userId, int expectedVersion) {
        MedicalRecord r = lockAndCheck(encounterId, expectedVersion);
        requireStatus(r, RecordStatus.PENDING_SIGNATURE, "lock the record");
        requireUser(userId);
        requireChecklist(encounterId); // kiểm tra lại điều kiện tại thời điểm khóa (V-08)
        snapshots.insertVersion(r.getRecordId(), r.getCurrentVersion(),
                snapshots.buildPayload(encounterId), DEV_ACTOR);
        r.lock(userId, DEV_ACTOR);
        audit.success("RECORD_DEMO_LOCKED", TARGET, r.getRecordId(), userId);
        return r;
    }

    /** Bổ sung sau khóa (IV7 M-14 / TX-05). Không sửa bản gốc. */
    @Transactional
    public Addendum createAddendum(UUID encounterId, String reason, String content, int expectedVersion) {
        MedicalRecord r = lockAndCheck(encounterId, expectedVersion);
        requireStatus(r, RecordStatus.LOCKED, "add an addendum");
        Addendum saved = addenda.save(Addendum.create(r.getRecordId(), reason.trim(), content.trim(), DEV_ACTOR));
        audit.success("ADDENDUM_CREATED", TARGET, r.getRecordId(), null);
        return saved;
    }

    @Transactional
    public List<Addendum> listAddenda(UUID encounterId) {
        MedicalRecord r = find(encounterId);
        auditLockedRead(r);
        return addenda.findByRecordIdOrderByCreatedAtAsc(r.getRecordId());
    }

    @Transactional
    public Addendum confirmAddendum(UUID encounterId, UUID addendumId, UUID userId) {
        MedicalRecord r = find(encounterId);
        Addendum a = addenda.findById(addendumId)
                .filter(x -> x.getRecordId().equals(r.getRecordId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Addendum not found"));
        if (a.getConfirmationStatus() != Addendum.Status.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Addendum already confirmed");
        }
        requireUser(userId);
        a.confirm(userId, DEV_ACTOR);
        return a;
    }

    /** V-09: hồ sơ không bao giờ bị xóa; ghi nhận nỗ lực xóa. */
    @Transactional
    public void rejectDelete(UUID encounterId) {
        MedicalRecord r = find(encounterId);
        audit.denied("RECORD_DELETE_DENIED", TARGET, r.getRecordId());
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Medical record cannot be deleted");
    }

    private MedicalRecord find(UUID encounterId) {
        return records.findByEncounterId(encounterId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical record not found"));
    }

    /** Khóa dòng rồi so khớp phiên bản; không có ghi đè "người ghi sau thắng" (V-11). */
    private MedicalRecord lockAndCheck(UUID encounterId, int expectedVersion) {
        MedicalRecord r = records.findForUpdateByEncounterId(encounterId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical record not found"));
        if (r.getRowVersion() != expectedVersion) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Record was modified (expected version " + expectedVersion + ", current "
                            + r.getRowVersion() + "); reload and retry");
        }
        return r;
    }

    private void requireStatus(MedicalRecord r, RecordStatus expected, String action) {
        if (r.getRecordStatus() != expected) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot " + action + " when record is " + r.getRecordStatus());
        }
    }

    /** Điều kiện nộp và khóa: lần khám đã hoàn tất và có chẩn đoán chính đã xác nhận. */
    private void requireChecklist(UUID encounterId) {
        MedicalEncounter e = encounters.findById(encounterId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
        if (e.getStatus() != EncounterStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Encounter must be COMPLETED before the record is submitted or locked");
        }
        if (!diagnoses.existsByEncounterIdAndDiagnosisTypeAndDiagnosisStatus(
                encounterId, DiagnosisType.PRIMARY, DiagnosisStatus.CONFIRMED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Record needs a confirmed primary diagnosis");
        }
    }

    private void requireUser(UUID userId) {
        if (!lookup.userExists(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    private void auditLockedRead(MedicalRecord r) {
        if (r.getRecordStatus() == RecordStatus.LOCKED || r.getRecordStatus() == RecordStatus.ARCHIVED) {
            audit.success("RECORD_READ", TARGET, r.getRecordId(), null);
        }
    }
}