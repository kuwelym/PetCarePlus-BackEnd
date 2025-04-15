package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import petitus.petcareplus.model.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByBookingId(UUID bookingId);

    Optional<Payment> findByTransactionCode(String transactionCode);
}