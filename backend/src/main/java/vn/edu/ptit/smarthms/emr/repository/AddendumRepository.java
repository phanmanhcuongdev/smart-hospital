package vn.edu.ptit.smarthms.emr.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.emr.domain.Addendum;

public interface AddendumRepository extends JpaRepository<Addendum, UUID> {
    List<Addendum> findByRecordIdOrderByCreatedAtAsc(UUID recordId);
}