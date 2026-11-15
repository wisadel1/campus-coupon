package com.campus.coupon.coupon.constant;

public class CouponRedisKeys {
    private CouponRedisKeys(){}

    public static String stockKey(Long templateId){
        return "coupon:stock:" + templateId;
    }
}
