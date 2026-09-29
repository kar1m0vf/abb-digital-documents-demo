package az.abb.embassyflow.order.dao.repository;

import az.abb.embassyflow.order.dao.entity.DocumentOrder;
import az.abb.embassyflow.order.enums.OrderStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentOrderRepository extends JpaRepository<DocumentOrder, Long> {

    boolean existsByVerificationCode(String verificationCode);

    List<DocumentOrder> findByCustomerIdOrderByIdDesc(Long customerId);

    Optional<DocumentOrder> findByOrderNumber(String orderNumber);

    List<DocumentOrder> findByEmbassyIdOrderByIdDesc(Long embassyId);

    long countByEmbassyId(Long embassyId);

    long countByEmbassyIdAndStatus(Long embassyId, OrderStatus status);

    long countByEmbassyIdAndStatusIn(Long embassyId, Collection<OrderStatus> statuses);
}