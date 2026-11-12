package com.campus.coupon.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.coupon.common.BusinessException;
import com.campus.coupon.common.PageResponse;
import com.campus.coupon.template.dto.TemplateCreateRequest;
import com.campus.coupon.template.dto.TemplateResponse;
import com.campus.coupon.template.entity.CouponTemplate;
import com.campus.coupon.template.mapper.CouponTemplateMapper;
import com.campus.coupon.template.service.CouponTemplateService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class CouponTemplateServiceImpl implements CouponTemplateService {
    @Resource
    private CouponTemplateMapper couponTemplateMapper;

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
        return template.getTemplateNo();
    }

    @Override
    public TemplateResponse getByTemplateNo(String templateNo){
        CouponTemplate template = couponTemplateMapper.selectOne(
                new LambdaQueryWrapper<CouponTemplate>().eq(CouponTemplate::getTemplateNo, templateNo)
        );
        if(template == null){
            throw new BusinessException(1001, "模板不存在");
        }
        return toResponse(template);
    }

    @Override
    public PageResponse<TemplateResponse> pageList(Long current, Long size){
        Page<CouponTemplate> page = new Page<>(current, size);
        Page<CouponTemplate> result = couponTemplateMapper.selectPage(page, null);

        List<TemplateResponse> records = result.getRecords().stream().map(this::toResponse).collect(Collectors.toList());

        return PageResponse.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
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
