package com.campus.coupon.coupon.task;

import com.campus.coupon.coupon.constant.CouponRedisKeys;
import com.campus.coupon.coupon.mapper.UserCouponMapper;
import com.campus.coupon.template.entity.CouponTemplate;
import com.campus.coupon.template.mapper.CouponTemplateMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class CouponReconcileTask {
    @Resource
    private CouponTemplateMapper couponTemplateMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    // TODO:此处检查应当设置一个时间点，对账检查过去的发放操作中mysql与redis是否保持一致，其次应当对当前余量进行检查，此时异常应当出现于初始化或者回溯，需要人工调整
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    private void reconcile(){
        List<CouponTemplate> templates = couponTemplateMapper.selectList(null);
        for(CouponTemplate t : templates){
            String key = CouponRedisKeys.stockKey(t.getId());
            String redisStock = stringRedisTemplate.opsForValue().get(key);
            if(redisStock == null){
                log.warn("模板[{}] Redis库存缺失，按DB剩余量补录", t.getTemplateNo());
                int remaining = Math.max(0, t.getTotalQuantity() - t.getIssuedQuantity());
                stringRedisTemplate.opsForValue().set(key, String.valueOf(remaining));
                continue;
            }
            long diff = Long.parseLong(redisStock) - (t.getTotalQuantity() - t.getIssuedQuantity());
            if(Math.abs(diff) > 0){
                log.warn("模板[{}] Redis库存与DB差异：redis{}，dbRemaining={}，diff={}",
                        t.getTemplateNo(), redisStock, t.getTotalQuantity() - t.getIssuedQuantity(),
                        diff);
            }
        }
    }
}
