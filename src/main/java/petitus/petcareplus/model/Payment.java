package petitus.petcareplus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import petitus.petcareplus.utils.enums.PaymentMethod;
import petitus.petcareplus.utils.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@Entity
@Table(name = "payments")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Payment extends AbstractBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "order_code", nullable = false)
    private String orderCode;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "payment_description")
    private String paymentDescription;

    // JSON column để lưu data riêng của từng gateway
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gateway_data", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> gatewayData = new HashMap<>();

    // Deprecated fields - keep for backward compatibility
    @Deprecated
    @Column(name = "transaction_code")
    private String transactionCode;

    @Deprecated
    @Column(name = "bank_code")
    private String bankCode;

    @Deprecated
    @Column(name = "card_type")
    private String cardType;

    // Helper methods for VNPay
    @JsonIgnore
    public void setVnpayData(String transactionCode, String bankCode, String cardType) {
        if (gatewayData == null) {
            gatewayData = new HashMap<>();
        }
        gatewayData.put("transaction_code", transactionCode);
        gatewayData.put("bank_code", bankCode);
        gatewayData.put("card_type", cardType);
    }

    @JsonIgnore
    public VnpayData getVnpayData() {
        if (gatewayData == null)
            return null;
        return VnpayData.builder()
                .transactionCode((String) gatewayData.get("transaction_code"))
                .bankCode((String) gatewayData.get("bank_code"))
                .cardType((String) gatewayData.get("card_type"))
                .build();
    }

    // Helper methods for PayOS
    @JsonIgnore
    public void setPayosData(String paymentLinkId, String checkoutUrl, String qrCode) {
        if (gatewayData == null) {
            gatewayData = new HashMap<>();
        }
        gatewayData.put("payment_link_id", paymentLinkId);
        gatewayData.put("checkout_url", checkoutUrl);
        gatewayData.put("qr_code", qrCode);
    }

    @JsonIgnore
    public PayosData getPayosData() {
        if (gatewayData == null)
            return null;
        return PayosData.builder()
                .paymentLinkId((String) gatewayData.get("payment_link_id"))
                .checkoutUrl((String) gatewayData.get("checkout_url"))
                .qrCode((String) gatewayData.get("qr_code"))
                .build();
    }

    // Data classes for type safety
    @Builder
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VnpayData {
        private String transactionCode;
        private String bankCode;
        private String cardType;
    }

    @Builder
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PayosData {
        private String paymentLinkId;
        private String checkoutUrl;
        private String qrCode;
    }
}