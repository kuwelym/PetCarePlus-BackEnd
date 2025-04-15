package petitus.petcareplus.dto.response.payment;

import petitus.petcareplus.utils.enums.VnpayReturnStatus;

public class VnpayReturnResponse {
    private String amount;
    private String bankCode;
    private String cardType;
    private String orderInfo;
    private String payDate;
    private String responseCode;
    private String message;
    private VnpayReturnStatus status;

    public VnpayReturnResponse() {
    }

    public VnpayReturnResponse(
            String amount, String bankCode, String cardType, String orderInfo,
            String payDate, String responseCode, String message, VnpayReturnStatus status) {
        this.amount = amount;
        this.bankCode = bankCode;
        this.cardType = cardType;
        this.orderInfo = orderInfo;
        this.payDate = payDate;
        this.responseCode = responseCode;
        this.message = message;
        this.status = status;
    }

    // Getters and Setters

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getOrderInfo() {
        return orderInfo;
    }

    public void setOrderInfo(String orderInfo) {
        this.orderInfo = orderInfo;
    }

    public String getPayDate() {
        return payDate;
    }

    public void setPayDate(String payDate) {
        this.payDate = payDate;
    }

    public VnpayReturnStatus getStatus() {
        return status;
    }

    public void setStatus(VnpayReturnStatus status) {
        this.status = status;
    }
}
