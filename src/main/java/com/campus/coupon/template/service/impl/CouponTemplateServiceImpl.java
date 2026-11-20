package com.campus.coupon.template.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.coupon.common.BusinessException;
import com.campus.coupon.common.PageResponse;
import com.campus.coupon.coupon.constant.CouponRedisKeys;
import com.campus.coupon.template.constant.TemplateRedisKeys;
import com.campus.coupon.template.dto.TemplateCreateRequest;
import com.campus.coupon.template.dto.TemplateResponse;
import com.campus.coupon.template.entity.CouponTemplate;
import com.campus.coupon.template.mapper.CouponTemplateMapper;
import com.campus.coupon.template.service.CouponTemplateService;
import jakarta.annotation.Resource;
import org.springframework.cache.Cache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;



@Service
public class CouponTemplateServiceImpl implements CouponTemplateService {
    @Resource
    private CouponTemplateMapper couponTemplateMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${app.cache.template-detail-enabled:true}")
    private boolean templateDetailCacheEnabled;

    @Override
    public String createTemplate(TemplateCreateRequest request){
        if(!request.getValidStart().isBefore(request.getValidEnd())){
            throw new BusinessException("有效期的开始时间必须早于结束时间");
        }

        CouponTemplate template = new CouponTemplate();
        template.setTemplateNo(generateTemplateNo());
        template.setName(request.getName());
        template.setTotalQuantity(request.getTotalQuantity());
        template.setIssuedQuantity(0);
        template.setValidStart(request.getValidStart());
        template.setValidEnd(request.getValidEnd());
        template.setStatus(1);
        template.setCreatedAt(LocalDateTime.now());

        couponTemplateMapper.insert(template);
        //在数据库操作完成且成功后进行redis插入，避免数据库提交失败回滚而redis提交成功造成的数据差异
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit(){
                        stringRedisTemplate.opsForValue().set(
                                CouponRedisKeys.stockKey(template.getId()),
                                String.valueOf(template.getTotalQuantity())
                        );
                    }
                }
        );
        return template.getTemplateNo();
    }

    @Override
    public TemplateResponse getByTemplateNo(String templateNo) {

        String key = TemplateRedisKeys.detailKey(templateNo);

        // 1. 开启缓存时，先查询 Redis
        if (templateDetailCacheEnabled) {
            String cached = stringRedisTemplate.opsForValue().get(key);

            if (cached != null) {
                return JSON.parseObject(cached, TemplateResponse.class);
            }
        }

        // 2. 查询数据库
        CouponTemplate template = couponTemplateMapper.selectOne(
                new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getTemplateNo, templateNo)
        );

        if (template == null) {
            throw new BusinessException(1001, "模板不存在");
        }

        TemplateResponse response = toResponse(template);

        // 3. 只有开启缓存时，才写入 Redis
        if (templateDetailCacheEnabled) {
            stringRedisTemplate.opsForValue().set(
                    key,
                    JSON.toJSONString(response),
                    Duration.ofMinutes(10)
            );
        }

        return response;
    }

    @Override
    public PageResponse<TemplateResponse> pageList(Long current, Long size){
        Page<CouponTemplate> page = new Page<>(current, size);
        Page<CouponTemplate> result = couponTemplateMapper.selectPage(page, null);

        List<TemplateResponse> records = result.getRecords().stream().map(this::toResponse).collect(Collectors.toList());

        return PageResponse.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }



    @Override
    @Transactional
    public void updateTemplate(String templateNo, TemplateCreateRequest request){
        CouponTemplate existing = couponTemplateMapper.selectOne(
                new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getTemplateNo, templateNo)
        );
        if(existing == null){
            throw new BusinessException(1001, "模板不存在");
        }

        existing.setName(request.getName());
        existing.setTotalQuantity(request.getTotalQuantity());
        existing.setValidStart(request.getValidStart());
        existing.setValidEnd(request.getValidEnd());
        couponTemplateMapper.updateById(existing);

        //TODO: 这里还有在第一条插入数据库后取出，然后第二条插入，导致缓存与数据库不匹配的问题，并且这里没有对删除缓存失败进行处理
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        stringRedisTemplate.delete(TemplateRedisKeys.detailKey(templateNo));
                    }
                }
        );
    }

    private TemplateResponse toResponse(CouponTemplate template){
        TemplateResponse resp = new TemplateResponse();
        resp.setTemplateNo(template.getTemplateNo());
        resp.setName(template.getName());
        resp.setTotalQuantity(template.getTotalQuantity());
        resp.setIssuedQuantity(template.getIssuedQuantity());
        resp.setValidStart(template.getValidStart());
        resp.setValidEnd(template.getValidEnd());
        resp.setStatus(template.getStatus());
        return resp;
    }

    private String generateTemplateNo(){
        return "T" + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
