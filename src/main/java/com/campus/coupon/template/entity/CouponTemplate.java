package com.campus.coupon.template.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("coupon_template")
public class CouponTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateNo;

    private String name;

    private Integer totalQuantity;

    private Integer issuedQuantity;

    private LocalDateTime validStart;

    private LocalDateTime validEnd;

    private Integer status;

    private LocalDateTime createdAt;
}
