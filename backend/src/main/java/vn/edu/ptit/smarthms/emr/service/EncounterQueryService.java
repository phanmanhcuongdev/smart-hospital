package vn.edu.ptit.smarthms.emr.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;

@Service
public class EncounterQueryService {
    private final MedicalEncounterRepository repository;

    public EncounterQueryService(MedicalEncounterRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public Page<MedicalEncounter> search(UUID patientId, EncounterStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        if (patientId != null && status != null) {
            return repository.findByPatientIdAndStatus(patientId, status, pageable);
        }
        if (patientId != null) {
            return repository.findByPatientId(patientId, pageable);
        }
        if (status != null) {
            return repository.findByStatus(status, pageable);
        }
        return repository.findAll(pageable);
    }
}