package com.campus.coupon.template.controller;

import com.campus.coupon.common.PageResponse;
import com.campus.coupon.common.Result;
import com.campus.coupon.template.dto.TemplateCreateRequest;
import com.campus.coupon.template.dto.TemplateResponse;
import com.campus.coupon.template.service.CouponTemplateService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/template")
public class CouponTemplateController {
    @Resource
    private CouponTemplateService couponTemplateService;

    @PostMapping("/create")
    public Result<String> create(@Valid @RequestBody TemplateCreateRequest request){
        return Result.success(couponTemplateService.createTemplate(request));
    }

    @GetMapping("/{templateNo}")
    public Result<TemplateResponse> detail(@PathVariable String templateNo){
        return Result.success(couponTemplateService.getByTemplateNo(templateNo));
    }

    @GetMapping("/page")
    public Result<PageResponse<TemplateResponse>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size){
        return Result.success(couponTemplateService.pageList(current, size));
    }
}
