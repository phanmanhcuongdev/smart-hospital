package vn.edu.ptit.smarthms.emr.api;

import java.time.OffsetDateTime;
import java.util.UUID;
import vn.edu.ptit.smarthms.emr.domain.Diagnosis;

public record DiagnosisResponse(UUID diagnosisId, UUID encounterId, String diagnosisType,
                                String icd10Code, String narrative, String diagnosisStatus,
                                UUID confirmedBy, OffsetDateTime createdAt) {
    public static DiagnosisResponse from(Diagnosis d) {
        return new DiagnosisResponse(d.getDiagnosisId(), d.getEncounterId(),
                d.getDiagnosisType().name(), d.getIcd10Code(), d.getNarrative(),
                d.getDiagnosisStatus().name(), d.getConfirmedBy(), d.getCreatedAt());
    }
}