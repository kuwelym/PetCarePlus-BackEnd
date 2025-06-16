package petitus.petcareplus.utils.enums;

public enum PaymentMethod {
    VNPAY("VNPay"),
    PAYOS("PayOS"),
    MOMO("MoMo"),
    ZALOPAY("ZaloPay"),
    BANK_TRANSFER("Bank Transfer"),
    CASH("Cash");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}