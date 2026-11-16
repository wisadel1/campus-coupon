package com.campus.coupon.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderResponse {
    private String orderNo;
    private String couponNo;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}
