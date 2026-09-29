package az.abb.embassyflow.customer.dao.repository;

import az.abb.embassyflow.customer.dao.entity.Card;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {

    boolean existsByIdAndAccount_CustomerIdAndActiveTrue(Long id, Long customerId);

    Optional<Card> findByIdAndAccount_CustomerIdAndActiveTrue(Long id, Long customerId);

    Optional<Card> findFirstByAccount_CustomerIdAndActiveTrueOrderByIdAsc(Long customerId);
}
