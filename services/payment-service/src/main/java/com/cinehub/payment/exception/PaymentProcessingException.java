package com.cinehub.payment.exception; // Đặt vào package com.cinehub.payment.exception

/**
 * Custom Exception cho các lỗi nghiệp vụ trong quá trình xử lý thanh toán,
 * ví dụ: giao dịch không tồn tại, trạng thái không hợp lệ, hoặc lỗi từ Gateway.
 */
public class PaymentProcessingException extends RuntimeException {

    // Thêm trường code lỗi nếu cần phân biệt rõ hơn
    // private final String errorCode;

    public PaymentProcessingException(String message) {
        super(message);
    }

    // Thêm Constructor để lưu nguyên nhân gốc (stack trace)
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    // Thêm Constructor có code lỗi (nếu cần)
    /*
     * public PaymentProcessingException(String message, String errorCode) {
     * super(message);
     * this.errorCode = errorCode;
     * }
     */
}