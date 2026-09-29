package az.abb.embassyflow.presentation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresentationOrderRepository extends JpaRepository<PresentationOrder, Long> {
    Optional<PresentationOrder> findByRequestKey(String key);
    Optional<PresentationOrder> findByOrderNumber(String number);
    List<PresentationOrder> findAllByOrderByIdDesc();
}
