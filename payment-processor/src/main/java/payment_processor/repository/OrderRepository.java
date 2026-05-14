package payment_processor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import payment_processor.entity.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    @Query("SELECT o from Order o where o.status = 'PENDING' and o.createdAt < :cutoffTime")
    List<Order> findStalePendingOrders(@Param("cutoffTime")LocalDateTime cutoffTime);
}
