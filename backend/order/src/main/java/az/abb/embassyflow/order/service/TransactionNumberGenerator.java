package az.abb.embassyflow.order.service;

import jakarta.persistence.EntityManager;
import java.time.Year;
import org.springframework.stereotype.Component;

@Component
public class TransactionNumberGenerator {

    private static final String FORMAT = "TXN-%d-%05d";

    private final EntityManager entityManager;

    public TransactionNumberGenerator(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public String next() {
        Long sequence = ((Number) entityManager
                .createNativeQuery("SELECT nextval('txn_no_seq')")
                .getSingleResult()).longValue();
        return String.format(FORMAT, Year.now().getValue(), sequence);
    }
}
