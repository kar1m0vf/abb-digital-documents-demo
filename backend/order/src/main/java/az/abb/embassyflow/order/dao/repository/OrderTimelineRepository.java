package az.abb.embassyflow.order.dao.repository;

import az.abb.embassyflow.order.dao.entity.OrderTimeline;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderTimelineRepository extends JpaRepository<OrderTimeline, Long> {
}