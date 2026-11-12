package com.campus.coupon.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;

    public BusinessException(Integer code, String message){
        super(message);
        this.code = code;
    }

    public BusinessException(String message){
        this(1000, message);
    }
}
