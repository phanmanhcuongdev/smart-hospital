package vn.edu.ptit.smarthms.emr.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddendumRepository extends JpaRepository<Addendum, UUID> {
    List<Addendum> findByRecordId(UUID recordId);
}
