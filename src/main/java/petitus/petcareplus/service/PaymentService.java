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

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";
        long amountInVnd = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        // String bankCode = req.getParameter("bankCode");

        String vnp_TxnRef = generateTransactionCode();
        payment.setTransactionCode(vnp_TxnRef);
        paymentRepository.save(payment);

        String vnp_IpAddr = "127.0.0.1";

        String vnp_TmnCode = vnPayConfig.getTmnCode();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amountInVnd));
        vnp_Params.put("vnp_CurrCode", "VND");

        // if (bankCode != null && !bankCode.isEmpty()) {
        // vnp_Params.put("vnp_BankCode", bankCode);
        // }
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", orderType);

        String locate = "vn";
        if (locate != null && !locate.isEmpty()) {
            vnp_Params.put("vnp_Locale", locate);
        } else {
            vnp_Params.put("vnp_Locale", "vn");
        }
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnPayConfig.getPayUrl() + "?" + queryUrl;

        return paymentUrl;
    }

    public static String hmacSHA512(final String key, final String data) {

        log.info("--key: {}", key);
        log.info("--data: {}", data);

        try {

            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();

        } catch (Exception ex) {
            return "";
        }
    }

    // private String bytesToHex(byte[] bytes) {
    // StringBuilder sb = new StringBuilder();
    // for (byte b : bytes) {
    // sb.append(String.format("%02x", b));
    // }
    // return sb.toString();
    // }

    // private String getIpAddress(HttpServletRequest request) {
    // String ipAdress;
    // try {
    // ipAdress = request.getHeader("X-FORWARDED-FOR");
    // if (ipAdress == null) {
    // ipAdress = request.getRemoteAddr();
    // }
    // } catch (Exception e) {
    // ipAdress = "Invalid IP:" + e.getMessage();
    // }
    // return ipAdress;
    // }

    private String generateTransactionCode() {
        // Create a format like: yyyyMMdd + 6 random digits
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String dateString = dateFormat.format(new Date());

        // Generate 6 random digits
        Random random = new Random();
        int randomNum = random.nextInt(900000) + 100000; // 6-digit number

        return dateString + randomNum;
    }

    public String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                sb.append(fieldName);
                sb.append("=");
                sb.append(fieldValue);
            }
            if (itr.hasNext()) {
                sb.append("&");
            }
        }
        return hmacSHA512(vnPayConfig.getHashSecret(), sb.toString());
    }

    // @Transactional
    // public PaymentResponse processVnpayReturn(Map<String, String> vnpayResponse)
    // {
    // String vnpSecureHash = vnpayResponse.get("vnp_SecureHash");
    // String vnpTxnRef = vnpayResponse.get("vnp_TxnRef");
    // String vnpResponseCode = vnpayResponse.get("vnp_ResponseCode");
    // // String vnpTransactionNo = vnpayResponse.get("vnp_TransactionNo");
    // String vnpBankCode = vnpayResponse.get("vnp_BankCode");
    // String vnpCardType = vnpayResponse.get("vnp_CardType");
    // String vnpAmount = vnpayResponse.get("vnp_Amount");
    // // String vnpOrderInfo = vnpayResponse.get("vnp_OrderInfo");

    // // Remove vnp_SecureHash from params to verify
    // Map<String, String> verifyParams = new HashMap<>(vnpayResponse);
    // verifyParams.remove("vnp_SecureHash");
    // verifyParams.remove("vnp_SecureHashType");

    // // Sort params
    // Map<String, String> sortedParams = new TreeMap<>(verifyParams);

    // String signValue = hashAllFields(sortedParams);

    // // Find payment by transaction code
    // Payment payment = paymentRepository.findByTransactionCode(vnpTxnRef)
    // .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

    // // Check if payment amount matches
    // // VNPay amount includes 00 at the end (x100)
    // long expectedAmount =
    // payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
    // long actualAmount = Long.parseLong(vnpAmount);

    // if (expectedAmount != actualAmount) {
    // throw new BadRequestException("Payment amount mismatch");
    // }

    // // Update payment status based on response code
    // if (signValue.equals(vnpSecureHash)) {
    // if ("00".equals(vnpResponseCode)) {
    // payment.setStatus(PaymentStatus.COMPLETED);
    // payment.setPaymentDate(LocalDateTime.now());
    // } else {
    // payment.setStatus(PaymentStatus.FAILED);
    // }
    // } else {
    // log.error("Invalid secure hash: expected {}, got {}", signValue,
    // vnpSecureHash);
    // }

    // payment.setBankCode(vnpBankCode);
    // payment.setCardType(vnpCardType);
    // payment.setUpdatedAt(LocalDateTime.now());

    // log.info("Payment status updated: {} - {}", payment.getId(),
    // payment.getStatus());

    // paymentRepository.save(payment);

    // // Return response
    // return mapToPaymentResponse(payment);
    // }

    @Transactional
    public PaymentResponse processVnpayReturn(Map<String, String> vnpayResponse) {

        log.info("--Processing VNPAY return with response: {}", vnpayResponse);

        // Extract parameters from the response
        String vnpBankCode = vnpayResponse.get("vnp_BankCode");
        String vnpCardType = vnpayResponse.get("vnp_CardType");
        String vnpTxnRef = vnpayResponse.get("vnp_TxnRef");

        String vnp_SecureHash = vnpayResponse.get("vnp_SecureHash");
        if (vnpayResponse.containsKey("vnp_SecureHashType")) {
            vnpayResponse.remove("vnp_SecureHashType");
        }
        if (vnpayResponse.containsKey("vnp_SecureHash")) {
            vnpayResponse.remove("vnp_SecureHash");
        }
        String signValue = hashAllFields(vnpayResponse);

        // Find payment by transaction code
        Payment payment = paymentRepository.findByTransactionCode(vnpTxnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Update payment status based on response code
        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(vnpayResponse.get("vnp_ResponseCode"))) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setPaymentDate(LocalDateTime.now());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
            }
        } else {
            log.error("Invalid secure hash: expected {}, got {}", signValue, vnp_SecureHash);
            // return new BadRequestException("Invalid secure hash");
            throw new BadRequestException(messageSourceService.get("invalid_secure_hash"));
        }

        payment.setBankCode(vnpBankCode);
        payment.setCardType(vnpCardType);
        payment.setUpdatedAt(LocalDateTime.now());

        log.info("Payment status updated: {} - {}", payment.getId(), payment.getStatus());

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