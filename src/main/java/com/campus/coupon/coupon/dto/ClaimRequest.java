package com.campus.coupon.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClaimRequest {
    @NotBlank(message = "模板编号不能为空")
    private String templateNo;

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
