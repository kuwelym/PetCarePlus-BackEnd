package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import petitus.petcareplus.configuration.VNPayConfig;
import petitus.petcareplus.dto.request.payment.CreatePaymentRequest;
import petitus.petcareplus.dto.response.payment.PaymentResponse;
import petitus.petcareplus.dto.response.payment.PaymentUrlResponse;
import petitus.petcareplus.dto.response.payment.VnpayReturnResponse;
import petitus.petcareplus.utils.enums.PaymentMethod;
import petitus.petcareplus.utils.enums.PaymentStatus;
import petitus.petcareplus.utils.enums.VnpResponseCode;
import petitus.petcareplus.utils.enums.VnpayReturnStatus;
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
        payment.setOrderCode(vnp_TxnRef);
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

    public VnpayReturnResponse verifyVnpayReturn(Map<String, String> params) {

        String vnp_SecureHash = params.get("vnp_SecureHash");

        if (params.containsKey("vnp_SecureHashType")) {
            params.remove("vnp_SecureHashType");
        }
        if (params.containsKey("vnp_SecureHash")) {
            params.remove("vnp_SecureHash");
        }
        String signValue = hashAllFields(params);

        VnpayReturnResponse response = new VnpayReturnResponse();

        if (signValue.equals(vnp_SecureHash)) {
            response.setAmount(params.get("vnp_Amount"));
            response.setBankCode(params.get("vnp_BankCode"));
            response.setCardType(params.get("vnp_CardType"));
            response.setOrderInfo(params.get("vnp_OrderInfo"));
            response.setPayDate(params.get("vnp_PayDate"));
            response.setResponseCode(params.get("vnp_ResponseCode"));

            if ("00".equals(params.get("vnp_ResponseCode"))) {
                response.setStatus(VnpayReturnStatus.SUCCESS);
            } else {
                response.setStatus(VnpayReturnStatus.FAIL);
            }

            response.setMessage(VnpResponseCode.getDescription(params.get("vnp_ResponseCode")));
        } else {
            response.setStatus(VnpayReturnStatus.INVALID_SIGNATURE);
            response.setMessage("Chữ ký không hợp lệ");
        }

        return response;
    }

    // handle ipn url
    public ResponseEntity<Map<String, String>> handleIPNUrl(Map<String, String> params) {
        String vnp_SecureHash = params.get("vnp_SecureHash");

        if (params.containsKey("vnp_SecureHashType")) {
            params.remove("vnp_SecureHashType");
        }
        if (params.containsKey("vnp_SecureHash")) {
            params.remove("vnp_SecureHash");
        }
        String signValue = hashAllFields(params);
        Map<String, String> response = new HashMap<>();

        if (!signValue.equals(vnp_SecureHash)) {
            response.put("RspCode", "97");
            response.put("Message", "Invalid Checksum");
            return ResponseEntity.ok(response);
        }

        // Check vnp_TxnRef if exists in DB
        String vnp_TxnRef = params.get("vnp_TxnRef");

        Optional<Payment> paymentOptional = paymentRepository.findByTransactionCode(vnp_TxnRef);
        if (paymentOptional.isEmpty()) {
            response.put("RspCode", "01");
            response.put("Message", "Payment not found");
            return ResponseEntity.ok(response);
        }

        Payment payment = paymentOptional.get();

        // Check amount
        Long amount = Long.parseLong(params.get("vnp_Amount"));
        if (amount != payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue()) {
            response.put("RspCode", "04");
            response.put("Message", "Invalid amount");
            return ResponseEntity.ok(response);
        }

        // Check status
        if (payment.getStatus() != PaymentStatus.PENDING) {
            response.put("RspCode", "02");
            response.put("Message", "Payment already confirmed");
            return ResponseEntity.ok(response);
        }

        // Update payment status
        if ("00".equals(params.get("vnp_ResponseCode"))) {
            payment.setStatus(PaymentStatus.COMPLETED);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        payment.setVnpayData(vnp_TxnRef, params.get("vnp_BankCode"), params.get("vnp_CardType"));
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentDescription(params.get("vnp_OrderInfo"));

        paymentRepository.save(payment);

        // Update payment status of booking if payment is successful
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            Booking booking = payment.getBooking();
            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            bookingRepository.save(booking);
        }

        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        return ResponseEntity.ok(response);

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
                .gatewayData(payment.getGatewayData())
                .orderCode(payment.getOrderCode())
                .paymentDate(payment.getPaymentDate())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

}