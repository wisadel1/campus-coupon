package com.campus.coupon.order.constant;

public class OrderRedisKeys {
    private OrderRedisKeys() {}

    public static String submitTokenKey(Long userId, String token){
        return "order:submit:" + userId + ":" + token;
    }
}
