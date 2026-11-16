package com.campus.coupon.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderCreateRequest {
    @NotBlank(message = "券号不能为空")
    private String couponNo;

    @NotBlank(message = "幂等Token不能为空")
    private String token;

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
