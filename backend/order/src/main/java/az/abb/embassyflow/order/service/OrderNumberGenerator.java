package az.abb.embassyflow.order.service;

import jakarta.persistence.EntityManager;
import java.time.Year;
import org.springframework.stereotype.Component;

@Component
public class OrderNumberGenerator {

    private static final String FORMAT = "AR-%d-%06d";

    private final EntityManager entityManager;

    public OrderNumberGenerator(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public String next() {
        Long sequence = ((Number) entityManager
                .createNativeQuery("SELECT nextval('order_no_seq')")
                .getSingleResult()).longValue();
        return String.format(FORMAT, Year.now().getValue(), sequence);
    }
}