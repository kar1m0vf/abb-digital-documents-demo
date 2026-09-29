package az.abb.embassyflow.presentation;

import jakarta.persistence.*;

/** Stores presentation-only options missing from the upstream contract. */
@Entity
@Table(name = "presentation_orders")
public class PresentationOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, unique = true) public String requestKey;
    @Column(nullable = false) public Long customerId;
    @Column(nullable = false, unique = true) public Long orderId;
    @Column(nullable = false, unique = true) public String orderNumber;
    @Column(nullable = false) public String status;
    @Column(nullable = false, length = 20000) public String payload;
    protected PresentationOrder() {}
}
