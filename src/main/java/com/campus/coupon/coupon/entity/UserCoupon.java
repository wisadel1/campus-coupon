package com.campus.coupon.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_coupon")
public class UserCoupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String couponNo;
    private Long templateId;
    private Long userId;
    private Integer status;
    private String orderNo;
    private LocalDateTime receivedTime;
    private LocalDateTime expireTime;
}
