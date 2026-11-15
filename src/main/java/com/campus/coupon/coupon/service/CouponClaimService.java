package com.campus.coupon.coupon.service;

import com.campus.coupon.coupon.dto.ClaimRequest;

public interface CouponClaimService {
    String claim(ClaimRequest request);
}
