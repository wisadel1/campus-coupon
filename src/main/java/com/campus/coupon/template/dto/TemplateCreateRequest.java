package com.campus.coupon.template.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TemplateCreateRequest {
    @NotBlank(message = "模板名称不能为空")
    private String name;

    @NotNull(message = "发放总量不能为空")
    @Min(value = 1, message = "发放总量必须大于0")
    private Integer totalQuantity;

    @NotNull(message = "有效期开始时间不能为空")
    private LocalDateTime validStart;

    @NotNull(message = "有效期结束时间不能为空")
    private LocalDateTime validEnd;
}
