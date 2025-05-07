package petitus.petcareplus.utils.enums;

public enum TransactionType {
    DEPOSIT,
    SERVICE_PROVIDER_EARNING, // Thu nhập từ dịch vụ
    WITHDRAWAL, // Rút tiền
    SYSTEM_ADJUSTMENT // Điều chỉnh từ hệ thống (hoàn tiền, điều chỉnh lỗi, etc.)
}