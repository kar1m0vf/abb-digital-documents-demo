package az.abb.embassyflow.embassy.dao.repository;

import az.abb.embassyflow.embassy.dao.entity.Embassy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmbassyRepository extends JpaRepository<Embassy, Long> {

    List<Embassy> findAllByActiveTrue();
}