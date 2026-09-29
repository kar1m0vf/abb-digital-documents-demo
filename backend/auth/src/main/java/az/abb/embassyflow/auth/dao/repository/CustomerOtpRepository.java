package az.abb.embassyflow.auth.dao.repository;

import az.abb.embassyflow.auth.dao.entity.CustomerOtp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOtpRepository extends JpaRepository<CustomerOtp, Long> {

    Optional<CustomerOtp> findFirstByCustomerIdOrderByIdDesc(Long customerId);

    void deleteByCustomerId(Long customerId);
}