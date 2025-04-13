package petitus.petcareplus.utils.enums;

public enum VnpResponseCode {
    SUCCESS("00", "Giao dịch thành công"),
    FRAUD("07", "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)."),
    NO_INTERNET_BANKING("09", "Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng."),
    WRONG_AUTH_3_TIMES("10", "Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần"),
    TIMEOUT("11", "Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch."),
    ACCOUNT_LOCKED("12", "Thẻ/Tài khoản của khách hàng bị khóa."),
    OTP_ERROR("13", "Sai mật khẩu xác thực giao dịch (OTP). Vui lòng thực hiện lại giao dịch."),
    USER_CANCEL("24", "Khách hàng hủy giao dịch"),
    INSUFFICIENT_FUNDS("51", "Tài khoản không đủ số dư để thực hiện giao dịch."),
    LIMIT_EXCEEDED("65", "Tài khoản đã vượt quá hạn mức giao dịch trong ngày."),
    BANK_MAINTENANCE("75", "Ngân hàng thanh toán đang bảo trì."),
    TOO_MANY_RETRIES("79", "Sai mật khẩu thanh toán quá số lần quy định."),
    UNKNOWN_ERROR("99", "Lỗi không xác định."),
    UNDEFINED("", "Không xác định");

    private final String code;
    private final String description;

    VnpResponseCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static String getDescription(String code) {
        for (VnpResponseCode value : values()) {
            if (value.code.equals(code)) {
                return value.description;
            }
        }
        return UNDEFINED.description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
