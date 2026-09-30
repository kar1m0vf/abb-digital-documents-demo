package az.abb.embassyflow.customer.dao.repository;

import az.abb.embassyflow.customer.dao.entity.Account;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @EntityGraph(attributePaths = "cards")
    List<Account> findByCustomerIdAndActiveTrueOrderById(Long customerId);

    @EntityGraph(attributePaths = "cards")
    List<Account> findByCustomerIdAndIdIn(Long customerId, Collection<Long> accountIds);

    boolean existsByIdAndCustomerIdAndActiveTrue(Long id, Long customerId);
}