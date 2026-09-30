package az.abb.embassyflow.embassy.dao.repository;

import az.abb.embassyflow.embassy.dao.entity.PortalUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortalUserRepository extends JpaRepository<PortalUser, Long> {

    Optional<PortalUser> findByUsernameAndActiveTrue(String username);

    Optional<PortalUser> findFirstByEmbassyIdAndActiveTrue(Long embassyId);
}