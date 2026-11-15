package com.campus.coupon.coupon.controller;

import com.campus.coupon.common.Result;
import com.campus.coupon.coupon.dto.ClaimRequest;
import com.campus.coupon.coupon.dto.ClaimResponse;
import com.campus.coupon.coupon.service.CouponClaimService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupon")
public class CouponClaimController {
    @Resource
    private CouponClaimService couponClaimService;

    @PostMapping("/claim")
    public Result<ClaimResponse> claim(@Valid @RequestBody ClaimRequest request){
        ClaimResponse response = new ClaimResponse();
        response.setCouponNo((couponClaimService.claim(request)));
        return Result.success(response);
    }
}
