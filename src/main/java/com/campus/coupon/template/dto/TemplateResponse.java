package com.campus.coupon.template.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TemplateResponse {
    private String templateNo;
    private String name;
    private Integer totalQuantity;
    private Integer issuedQuantity;
    private LocalDateTime validStart;
    private LocalDateTime validEnd;
    private Integer status;
}
