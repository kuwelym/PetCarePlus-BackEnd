package petitus.petcareplus.utils.enums;

public enum WithdrawalStatus {
    PENDING, // Chờ xử lý
    APPROVED, // Đã duyệt (chờ chuyển tiền)
    PROCESSING, // Đang xử lý chuyển tiền
    COMPLETED, // Hoàn thành
    REJECTED, // Từ chối
    FAILED // Thất bại
}