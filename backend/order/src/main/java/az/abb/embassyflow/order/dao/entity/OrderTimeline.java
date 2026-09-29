package az.abb.embassyflow.order.dao.entity;

import az.abb.embassyflow.common.dao.entity.AuditableEntity;
import az.abb.embassyflow.order.enums.TimelineStep;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_timeline")
public class OrderTimeline extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private DocumentOrder order;

    @Enumerated(EnumType.STRING)
    @Column(name = "step", nullable = false, length = 50)
    private TimelineStep step;

    public Long getId() {
        return id;
    }

    public DocumentOrder getOrder() {
        return order;
    }

    public void setOrder(DocumentOrder order) {
        this.order = order;
    }

    public TimelineStep getStep() {
        return step;
    }

    public void setStep(TimelineStep step) {
        this.step = step;
    }
}