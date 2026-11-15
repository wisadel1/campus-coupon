package com.campus.coupon.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.coupon.common.BusinessException;
import com.campus.coupon.coupon.constant.CouponRedisKeys;
import com.campus.coupon.coupon.dto.ClaimRequest;
import com.campus.coupon.coupon.entity.UserCoupon;
import com.campus.coupon.coupon.mapper.UserCouponMapper;
import com.campus.coupon.coupon.service.CouponClaimService;
import com.campus.coupon.template.entity.CouponTemplate;
import com.campus.coupon.template.mapper.CouponTemplateMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class CouponClaimServiceImpl implements CouponClaimService {
    @Resource
    private CouponTemplateMapper couponTemplateMapper;

    @Resource
    private UserCouponMapper userCouponMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisScript<Long> couponDeductScript;

    @Resource
    private TransactionTemplate transactionTemplate;



    @Override
    public String claim(ClaimRequest request){
        CouponTemplate template = couponTemplateMapper.selectOne(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getTemplateNo, request.getTemplateNo()));
        if(template == null){
            throw new BusinessException(1001, "模板不存在");
        }

        Long result = stringRedisTemplate.execute(couponDeductScript,
                Collections.singletonList(CouponRedisKeys.stockKey(template.getId())));

        if(result == null || result == 0L){
            throw new BusinessException(2001, "优惠券已领完");
        }

        try{
            UserCoupon userCoupon = buildUserCoupon(template, request.getUserId());
            transactionTemplate.executeWithoutResult(staus -> userCouponMapper.insert(userCoupon));
            return userCoupon.getCouponNo();
        }catch (DuplicateKeyException e){
            //补偿预扣的券数
            // TODO:需要新增对其他操作异常的处理，如数据库连接异常，执行异常，字段异常
            stringRedisTemplate.opsForValue().increment(CouponRedisKeys.stockKey(template.getId()));
            log.warn("用户重复领取，已回滚Redis库存：userId={}，templateId{}",
                    request.getUserId(), template.getId());
            throw new BusinessException(2002, "您已经领取过该优惠券");
        }
    }

    private UserCoupon buildUserCoupon(CouponTemplate template, Long userId){
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setCouponNo(generateCouponNo());
        userCoupon.setTemplateId(template.getId());
        userCoupon.setUserId(userId);
        userCoupon.setStatus(0);
        userCoupon.setReceivedTime(LocalDateTime.now());
        userCoupon.setExpireTime(template.getValidEnd());
        return userCoupon;
    }

    private String generateCouponNo(){
        return "C" + System.currentTimeMillis()
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
