/**
 * Mô tả: Truy vấn các bản bổ sung theo hồ sơ.
 * Người thực hiện: Trần Duy Hiếu
 * Ngày thực hiện: 09/11/2026
 */
package vn.edu.ptit.smarthms.emr.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.emr.domain.Addendum;

public interface AddendumRepository extends JpaRepository<Addendum, UUID> {
    List<Addendum> findByRecordIdOrderByCreatedAtAsc(UUID recordId);
}