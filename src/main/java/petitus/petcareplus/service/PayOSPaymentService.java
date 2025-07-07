package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import petitus.petcareplus.configuration.PayOSConfig;
import petitus.petcareplus.dto.request.payment.CreatePayOSPaymentRequest;
import petitus.petcareplus.dto.response.payment.PaymentUrlResponse;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.Booking;
import petitus.petcareplus.model.Payment;
import petitus.petcareplus.repository.BookingRepository;
import petitus.petcareplus.repository.PaymentRepository;
import petitus.petcareplus.utils.enums.PaymentMethod;
import petitus.petcareplus.utils.enums.PaymentStatus;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.PaymentLinkData;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOSPaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final MessageSourceService messageSourceService;
    private final PayOSConfig payOSConfig;
    private final PayOS payOS;
    private final UserService userService;

    @Transactional
    public PaymentUrlResponse createPayOSPayment(CreatePayOSPaymentRequest request) {
        try {

            UUID userId = userService.getCurrentUserId();
            // 1. Validate booking
            Booking booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("booking_not_found")));

            // 2. Ensure booking belongs to user
            if (!booking.getUser().getId().equals(userId)) {
                throw new BadRequestException(messageSourceService.get("not_your_booking"));
            }

            // 3. Check if there's already a pending payment
            List<Payment> pendingPayments = paymentRepository.findPendingPaymentsByBookingId(booking.getId());
            if (!pendingPayments.isEmpty()) {
                throw new BadRequestException(messageSourceService.get("payment_already_pending"));
            }

            // Ensure booking is not already paid
            if (booking.getPaymentStatus() == PaymentStatus.COMPLETED) {
                throw new BadRequestException(messageSourceService.get("booking_already_paid"));
            }

            // 4. Generate order code
            long orderCode = generateOrderCode();
            String orderCodeString = String.valueOf(orderCode);

            // 5. Create payment record
            Payment payment = Payment.builder()
                    .booking(booking)
                    .amount(request.getAmount())
                    .paymentMethod(PaymentMethod.PAYOS)
                    .status(PaymentStatus.PENDING)
                    .paymentDescription(request.getDescription())
                    .orderCode(orderCodeString)
                    .build();

            Payment savedPayment = paymentRepository.save(payment);

            // 6. Prepare PayOS payment data
            List<ItemData> items = createPayOSItems(request);

            PaymentData paymentData = PaymentData.builder()
                    .orderCode(orderCode)
                    .amount(request.getAmount().multiply(BigDecimal.valueOf(1)).intValue()) // Not convert
                    .description(request.getDescription())
                    .buyerName(request.getBuyerName())
                    .buyerEmail(request.getBuyerEmail())
                    .buyerPhone(request.getBuyerPhone())
                    .buyerAddress(request.getBuyerAddress())
                    .items(items)
                    .cancelUrl(payOSConfig.getCancelUrl())
                    .returnUrl(payOSConfig.getReturnUrl())
                    .build();

            // 7. Create payment link using PayOS SDK
            CheckoutResponseData checkoutResponse = payOS.createPaymentLink(paymentData);

            // 8. Update payment with PayOS response
            savedPayment.setPayosData(checkoutResponse.getPaymentLinkId(),
                    checkoutResponse.getCheckoutUrl(),
                    checkoutResponse.getQrCode());
            paymentRepository.save(savedPayment);

            booking.setPaymentStatus(savedPayment.getStatus());
            booking.setPayment(savedPayment);
            bookingRepository.save(booking);

            log.info("PayOS payment created successfully. Order code: {}, Payment ID: {}",
                    orderCodeString, savedPayment.getId());

            return PaymentUrlResponse.builder()
                    .status(checkoutResponse.getStatus())
                    .currency(checkoutResponse.getCurrency())
                    .amount(checkoutResponse.getAmount())
                    .paymentUrl(checkoutResponse.getCheckoutUrl())
                    .qrCode(checkoutResponse.getQrCode())
                    .orderCode(orderCodeString)
                    .message(messageSourceService.get("payment_created"))
                    .build();

        } catch (Exception e) {
            log.error("Error creating PayOS payment: ", e);
            throw new BadRequestException(e.getLocalizedMessage());
        }
    }

    private List<ItemData> createPayOSItems(CreatePayOSPaymentRequest request) {
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            return request.getItems().stream()
                    .map(item -> ItemData.builder()
                            .name(item.getName())
                            .quantity(item.getQuantity())
                            .price(item.getPrice().multiply(BigDecimal.valueOf(1)).intValue()) //
                            .build())
                    .toList();
        }

        // Default item
        return Arrays.asList(
                ItemData.builder()
                        .name("PetCarePlus Booking")
                        .quantity(1)
                        .price(request.getAmount().multiply(BigDecimal.valueOf(1)).intValue())
                        .build());
    }

    private long generateOrderCode() {
        // Generate a unique order code (current timestamp + random)
        long timestamp = System.currentTimeMillis() / 1000; // Unix timestamp
        Random random = new Random();
        int randomNum = random.nextInt(1000); // 3-digit random number

        return timestamp * 1000 + randomNum;
    }

    @Transactional
    public void updatePaymentStatus(String orderCode, String status) {
        Payment payment = paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with order code: " + orderCode));

        PaymentStatus oldStatus = payment.getStatus();

        switch (status.toUpperCase()) {
            case "PAID":
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setPaymentDate(LocalDateTime.now());

                // Update booking payment status
                Booking booking = payment.getBooking();
                booking.setPaymentStatus(petitus.petcareplus.utils.enums.PaymentStatus.COMPLETED);
                booking.setPayment(payment);
                bookingRepository.save(booking);

                log.info("Payment completed successfully. Order code: {}, Amount: {}",
                        orderCode, payment.getAmount());
                break;

            case "CANCELLED":
                payment.setStatus(PaymentStatus.CANCELLED);
                log.info("Payment cancelled. Order code: {}", orderCode);
                break;

            default:
                payment.setStatus(PaymentStatus.FAILED);
                log.warn("Payment failed with unknown status: {}. Order code: {}", status, orderCode);
                break;
        }

        paymentRepository.save(payment);

        log.info("Payment status updated from {} to {} for order code: {}",
                oldStatus, payment.getStatus(), orderCode);
    }

    @Transactional
    public ResponseEntity<Map<String, String>> handlePayOSWebhook(Webhook webhookBody) {
        try {
            // Verify webhook using PayOS SDK
            WebhookData webhookData = payOS.verifyPaymentWebhookData(webhookBody);

            log.info("PayOS webhook verified successfully:\n{}",
                    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(webhookData));

            Map<String, String> response = new HashMap<>();
            String orderCode = webhookData.getOrderCode().toString();
            int amount = webhookData.getAmount();
            String code = webhookData.getCode();
            String description = webhookData.getDescription();
            String transactionDateTime = webhookData.getTransactionDateTime();

            log.info(
                    "Processing PayOS webhook for order code: {}, amount: {}, code: {}, description: {}, transactionDateTime: {}",
                    orderCode, amount, code, description, transactionDateTime);

            Optional<Payment> paymentOptional = paymentRepository.findByOrderCode(orderCode);
            if (paymentOptional.isEmpty()) {
                response.put("Message", "Payment not found");
                return ResponseEntity.ok(response);
            }

            Payment payment = paymentOptional.get();

            // Check amount
            if (amount != payment.getAmount().intValue()) {
                response.put("Message", "Invalid amount");
                return ResponseEntity.ok(response);
            }

            // Check status
            if (payment.getStatus() != PaymentStatus.PENDING) {
                response.put("Message", "Payment already confirmed");
                return ResponseEntity.ok(response);
            }

            // Update payment status
            if ("00".equals(code)) {
                payment.setStatus(PaymentStatus.COMPLETED);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
            }

            LocalDateTime paymentDate = mapStringToLocalDateTime(transactionDateTime);

            payment.setPaymentDate(paymentDate);
            payment.setPaymentDescription(description);

            paymentRepository.save(payment);

            // Update payment status of booking if payment is successful
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                Booking booking = payment.getBooking();
                booking.setPaymentStatus(PaymentStatus.COMPLETED);
                booking.setPayment(payment);
                bookingRepository.save(booking);
            }

            log.info("Payment status updated successfully for order code: {}", orderCode);

            response.put("Message", "Confirm Success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error verifying PayOS webhook: ", e);
            throw new BadRequestException("Invalid PayOS webhook signature");
        }
    }

    public void cancelPaymentLink(String orderCode, String reason) {
        try {
            long orderCodeLong = Long.parseLong(orderCode);
            payOS.cancelPaymentLink(orderCodeLong, reason);

            // Update payment status in database
            Payment payment = paymentRepository.findByOrderCode(orderCode)
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Payment not found with order code: " + orderCode));

            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);

            log.info("PayOS payment link cancelled successfully. Order code: {}, Reason: {}", orderCode, reason);

        } catch (Exception e) {
            log.error("Error cancelling PayOS payment link: ", e);
            throw new BadRequestException("Không thể hủy link thanh toán PayOS: " + e.getMessage());
        }
    }

    public PaymentLinkData getPaymentLinkInfo(String orderCode) {
        try {
            long orderCodeLong = Long.parseLong(orderCode);
            return payOS.getPaymentLinkInformation(orderCodeLong);
        } catch (Exception e) {
            log.error("Error getting PayOS payment info: ", e);
            throw new BadRequestException("Không thể lấy thông tin thanh toán PayOS: " + e.getMessage());
        }
    }

    @Transactional
    public void checkAndUpdatePaymentStatus(String orderCode) {
        try {
            log.info("Checking PayOS payment status for order code: {}", orderCode);

            // Get payment info from PayOS
            PaymentLinkData paymentLinkData = getPaymentLinkInfo(orderCode);

            log.info("PayOS payment info retrieved:\n{}",
                    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(paymentLinkData));

            // Find payment in database
            Payment payment = paymentRepository.findByOrderCode(orderCode)
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Payment not found with order code: " + orderCode));

            PaymentStatus currentDbStatus = payment.getStatus();

            // Map PayOS status to our PaymentStatus
            PaymentStatus newStatus = mapPayOSStatusToPaymentStatus(paymentLinkData.getStatus());

            // Only update if status has changed
            if (currentDbStatus != newStatus) {
                log.info("Payment status changed from {} to {} for order code: {}",
                        currentDbStatus, newStatus, orderCode);

                payment.setStatus(newStatus);

                // Set payment date if completed
                // if (newStatus == PaymentStatus.COMPLETED && payment.getPaymentDate() == null)
                // {
                // payment.setPaymentDate(LocalDateTime.now());
                // }

                // Update booking payment status
                if (newStatus == PaymentStatus.COMPLETED) {
                    // Booking booking = payment.getBooking();
                    // booking.setPaymentStatus(petitus.petcareplus.utils.enums.PaymentStatus.COMPLETED);
                    // bookingRepository.save(booking);

                    // log.info("Booking payment status updated to COMPLETED for booking ID: {}",
                    // booking.getId());
                }

                paymentRepository.save(payment);

                Booking booking = payment.getBooking();
                booking.setPaymentStatus(payment.getStatus());
                booking.setPayment(payment);
                bookingRepository.save(booking);

            } else {
                log.info("Payment status unchanged ({}) for order code: {}", currentDbStatus, orderCode);
            }

        } catch (ResourceNotFoundException e) {
            log.error("Payment not found in database for order code: {}", orderCode);
            throw e;
        } catch (Exception e) {
            log.error("Error checking and updating PayOS payment status for order code: {}", orderCode, e);
            throw new BadRequestException("Failed to check PayOS payment status: " + e.getMessage());
        }
    }

    private PaymentStatus mapPayOSStatusToPaymentStatus(String payOSStatus) {
        if (payOSStatus == null) {
            return PaymentStatus.PENDING;
        }

        return switch (payOSStatus.toUpperCase()) {
            case "PAID" -> PaymentStatus.COMPLETED;
            case "CANCELLED" -> PaymentStatus.CANCELLED;
            case "PENDING" -> PaymentStatus.PENDING;
            case "FAILED" -> PaymentStatus.FAILED;
            default -> {
                log.warn("Unknown PayOS status: {}. Mapping to FAILED", payOSStatus);
                yield PaymentStatus.FAILED;
            }
        };
    }

    public LocalDateTime mapStringToLocalDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }

        try {
            // Case 1: ISO with offset, ví dụ: 2025-06-19T10:14:45+07:00
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateTimeStr);
            return offsetDateTime.toLocalDateTime();
        } catch (DateTimeParseException e) {
            // Ignore and try next
        }

        try {
            // Case 2: No offset, space instead of 'T', ví dụ: 2025-06-19 10:14:45
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (DateTimeParseException e) {
            // Ignore and try next
        }

        try {
            // Case 3: ISO format without offset (T separator), ví dụ: 2025-06-19T10:14:45
            return LocalDateTime.parse(dateTimeStr);
        } catch (DateTimeParseException e) {
            // Still fail
            throw new IllegalArgumentException("Unsupported datetime format: " + dateTimeStr, e);
        }
    }

}