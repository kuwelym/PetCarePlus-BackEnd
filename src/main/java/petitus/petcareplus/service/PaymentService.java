package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.configuration.VNPayConfig;
import petitus.petcareplus.dto.request.payment.CreatePaymentRequest;
import petitus.petcareplus.dto.response.payment.PaymentResponse;
import petitus.petcareplus.dto.response.payment.PaymentUrlResponse;
import petitus.petcareplus.utils.enums.PaymentMethod;
import petitus.petcareplus.utils.enums.PaymentStatus;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.Booking;
import petitus.petcareplus.model.Payment;
import petitus.petcareplus.repository.BookingRepository;
import petitus.petcareplus.repository.PaymentRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final MessageSourceService messageSourceService;
    private final VNPayConfig vnPayConfig;

    @Transactional
    public PaymentUrlResponse createVnPayUrl(UUID userId, CreatePaymentRequest request) {
        // 1. Validate booking
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("booking_not_found")));

        // 2. Ensure booking belongs to user
        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException(messageSourceService.get("not_your_booking"));
        }

        // 3. Create payment record
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(request.getAmount())
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .paymentDescription(request.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // 4. Generate VNPay URL
        String vnpayUrl = generateVnPayUrl(savedPayment);

        return PaymentUrlResponse.builder()
                .paymentUrl(vnpayUrl)
                .build();
    }

    private String generateVnPayUrl(Payment payment) {
        Map<String, String> vnpParams = new TreeMap<>();

        // Required parameters
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());

        // Convert amount to VND with no decimal points (multiply by 100 for cents)
        long amountInVnd = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        vnpParams.put("vnp_Amount", String.valueOf(amountInVnd));

        // Generate transaction code
        String transactionCode = generateTransactionCode();
        vnpParams.put("vnp_TxnRef", transactionCode);

        // Update payment with transaction code
        payment.setTransactionCode(transactionCode);
        paymentRepository.save(payment);

        // Payment Info
        String orderInfo = "Payment for booking " + payment.getBooking().getId();
        if (payment.getPaymentDescription() != null && !payment.getPaymentDescription().isEmpty()) {
            orderInfo += ": " + payment.getPaymentDescription();
        }
        vnpParams.put("vnp_OrderInfo", orderInfo);

        // Optional bank code
        // vnpParams.put("vnp_BankCode", "NCB");

        // Locale
        vnpParams.put("vnp_Locale", "vn");

        // Return URL (your endpoint that handles VNPay response)
        String returnUrl = vnPayConfig.getAppUrl() + vnPayConfig.getReturnUrl();
        vnpParams.put("vnp_ReturnUrl", returnUrl);

        // IP address (use a fixed one for testing)
        vnpParams.put("vnp_IpAddr", "127.0.0.1");

        // Create date in VNPAY format
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String createDate = formatter.format(new Date());
        vnpParams.put("vnp_CreateDate", createDate);

        // Set expiry date (15 minutes)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 15);
        String expireDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_ExpireDate", expireDate);

        // Build query string
        StringBuilder query = new StringBuilder();
        StringBuilder hashData = new StringBuilder();

        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                // Create URL query string
                query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                query.append("=");
                query.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                query.append("&");

                // Create hash data string
                hashData.append(entry.getKey());
                hashData.append("=");
                hashData.append(entry.getValue());
                hashData.append("&");
            }
        }

        // Remove last '&'
        if (query.length() > 0) {
            query.deleteCharAt(query.length() - 1);
        }

        if (hashData.length() > 0) {
            hashData.deleteCharAt(hashData.length() - 1);
        }

        // Create hmac signature
        String secureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        return vnPayConfig.getPayUrl() + "?" + query;
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmacSha512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            hmacSha512.init(secretKeySpec);
            byte[] hmacSha512Bytes = hmacSha512.doFinal(data.getBytes());
            return bytesToHex(hmacSha512Bytes);
        } catch (Exception e) {
            log.error("Error creating HMAC SHA512", e);
            throw new RuntimeException("Error creating HMAC SHA512", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String generateTransactionCode() {
        // Create a format like: yyyyMMdd + 6 random digits
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String dateString = dateFormat.format(new Date());

        // Generate 6 random digits
        Random random = new Random();
        int randomNum = random.nextInt(900000) + 100000; // 6-digit number

        return dateString + randomNum;
    }

    @Transactional
    public PaymentResponse processVnpayReturn(Map<String, String> vnpayResponse) {
        String vnpSecureHash = vnpayResponse.get("vnp_SecureHash");
        String vnpTxnRef = vnpayResponse.get("vnp_TxnRef");
        String vnpResponseCode = vnpayResponse.get("vnp_ResponseCode");
        String vnpTransactionNo = vnpayResponse.get("vnp_TransactionNo");
        String vnpBankCode = vnpayResponse.get("vnp_BankCode");
        String vnpCardType = vnpayResponse.get("vnp_CardType");
        String vnpAmount = vnpayResponse.get("vnp_Amount");
        String vnpOrderInfo = vnpayResponse.get("vnp_OrderInfo");

        // Remove vnp_SecureHash from params to verify
        Map<String, String> verifyParams = new HashMap<>(vnpayResponse);
        verifyParams.remove("vnp_SecureHash");
        verifyParams.remove("vnp_SecureHashType");

        // Sort params
        Map<String, String> sortedParams = new TreeMap<>(verifyParams);

        // Build hash data
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            hashData.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }

        // Remove last '&'
        if (hashData.length() > 0) {
            hashData.deleteCharAt(hashData.length() - 1);
        }

        // Verify secure hash
        String secureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        if (!secureHash.equals(vnpSecureHash)) {
            throw new BadRequestException("Invalid secure hash");
        }

        // Find payment by transaction code
        Payment payment = paymentRepository.findByTransactionCode(vnpTxnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Check if payment amount matches
        // VNPay amount includes 00 at the end (x100)
        long expectedAmount = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        long actualAmount = Long.parseLong(vnpAmount);

        if (expectedAmount != actualAmount) {
            throw new BadRequestException("Payment amount mismatch");
        }

        // Update payment status based on response code
        if ("00".equals(vnpResponseCode)) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaymentDate(LocalDateTime.now());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        payment.setBankCode(vnpBankCode);
        payment.setCardType(vnpCardType);
        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        // Return response
        return mapToPaymentResponse(payment);
    }

    public List<PaymentResponse> getBookingPayments(UUID userId, UUID bookingId) {
        // Validate booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("booking_not_found")));

        // Ensure booking belongs to user or provider
        if (!booking.getUser().getId().equals(userId) && !booking.getProvider().getId().equals(userId)) {
            throw new BadRequestException(messageSourceService.get("not_authorized_for_booking"));
        }

        // Find all payments for booking
        List<Payment> payments = paymentRepository.findByBookingId(bookingId);

        return payments.stream()
                .map(this::mapToPaymentResponse)
                .toList();
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .transactionCode(payment.getTransactionCode())
                .bankCode(payment.getBankCode())
                .paymentDate(payment.getPaymentDate())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}