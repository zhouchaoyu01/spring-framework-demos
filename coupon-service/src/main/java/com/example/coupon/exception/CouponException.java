package com.example.coupon.exception;

/**
 * 优惠券业务异常类
 */
public class CouponException extends RuntimeException {

    private String errorCode;

    public CouponException(String message) {
        super(message);
    }

    public CouponException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public CouponException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getErrorCode() {
        return errorCode;
    }
}