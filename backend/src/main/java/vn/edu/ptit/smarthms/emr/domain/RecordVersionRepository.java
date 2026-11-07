package vn.edu.ptit.smarthms.emr.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordVersionRepository extends JpaRepository<RecordVersion, RecordVersion.Key> {
    List<RecordVersion> findByIdRecordIdOrderByIdVersionNoAsc(UUID recordId);
}
