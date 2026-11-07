package vn.edu.ptit.smarthms.hr.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleProfileRepository extends JpaRepository<RoleProfile, UUID> {
    Optional<RoleProfile> findByTitleName(String titleName);
    boolean existsByTitleName(String titleName);
}
