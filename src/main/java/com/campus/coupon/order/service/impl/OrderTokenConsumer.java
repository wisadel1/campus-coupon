package com.campus.coupon.order.service.impl;

import com.campus.coupon.common.BusinessException;
import com.campus.coupon.order.constant.OrderRedisKeys;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderTokenConsumer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void consumerOrThrow(Long userId, String token){
        Boolean deleted = stringRedisTemplate.delete(OrderRedisKeys.submitTokenKey(userId, token));
        if(!deleted){
            throw new BusinessException(3001, "请勿重复提交");
        }
    }
}
