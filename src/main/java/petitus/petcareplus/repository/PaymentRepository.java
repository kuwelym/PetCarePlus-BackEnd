package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import petitus.petcareplus.model.Payment;
import petitus.petcareplus.utils.enums.PaymentMethod;
import petitus.petcareplus.utils.enums.PaymentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

        // Primary method for finding by order code
        Optional<Payment> findByOrderCode(String orderCode);

        // For VNPay compatibility - sử dụng native query thay vì HQL
        @Query(value = "SELECT * FROM payments p WHERE p.transaction_code = :transactionCode OR " +
                        "(p.gateway_data IS NOT NULL AND p.gateway_data::jsonb ->> 'transaction_code' = :transactionCode)", nativeQuery = true)
        Optional<Payment> findByTransactionCode(@Param("transactionCode") String transactionCode);

        // Alternative method using JPQL function
        @Query("SELECT p FROM Payment p WHERE p.transactionCode = :transactionCode OR " +
                        "function('jsonb_extract_path_text', p.gatewayData, 'transaction_code') = :transactionCode")
        Optional<Payment> findByTransactionCodeJPQL(@Param("transactionCode") String transactionCode);

        // Find by booking
        List<Payment> findByBookingId(UUID bookingId);

        // Find by payment method
        List<Payment> findByPaymentMethod(PaymentMethod paymentMethod);

        // Find by status
        List<Payment> findByStatus(PaymentStatus status);

        // Find pending payments for a specific booking
        @Query("SELECT p FROM Payment p WHERE p.booking.id = :bookingId AND p.status = 'PENDING'")
        List<Payment> findPendingPaymentsByBookingId(@Param("bookingId") UUID bookingId);

        // Additional methods for PayOS
        @Query(value = "SELECT * FROM payments p WHERE p.order_code = :orderCode OR " +
                        "(p.gateway_data IS NOT NULL AND p.gateway_data::jsonb ->> 'order_code' = :orderCode)", nativeQuery = true)
        Optional<Payment> findByPayOSOrderCode(@Param("orderCode") String orderCode);
}