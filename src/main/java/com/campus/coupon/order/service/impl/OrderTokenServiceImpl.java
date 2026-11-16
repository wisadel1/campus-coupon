package com.campus.coupon.order.service.impl;

import com.campus.coupon.order.constant.OrderRedisKeys;
import com.campus.coupon.order.service.OrderTokenService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class OrderTokenServiceImpl implements OrderTokenService {
    private static final Duration TOKEN_TTL = Duration.ofSeconds(60);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String generate(Long userId){
        // TODO: 需要考虑token重复
        String token = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(
                OrderRedisKeys.submitTokenKey(userId, token),
                "1",
                TOKEN_TTL
        );
        return token;
    }
}
