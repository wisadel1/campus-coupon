package com.campus.coupon.coupon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

@Configuration
public class RedisScriptConfig {
    @Bean
    public RedisScript<Long> couponDeductScript() throws Exception{
        ClassPathResource resource = new ClassPathResource("lua/coupon_deduct.lua");
        String scriptText = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(scriptText);
        script.setResultType(Long.class);
        return script;
    }
}
