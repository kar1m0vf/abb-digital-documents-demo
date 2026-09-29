package az.abb.embassyflow.order.dao.repository;

import az.abb.embassyflow.order.dao.entity.Payment;
import az.abb.embassyflow.order.enums.PaymentStatus;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByOrderIdIn(Collection<Long> orderIds, Pageable pageable);

    Page<Payment> findByOrderIdInAndStatus(Collection<Long> orderIds, PaymentStatus status, Pageable pageable);
}
