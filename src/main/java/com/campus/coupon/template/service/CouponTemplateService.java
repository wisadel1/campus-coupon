package com.campus.coupon.template.service;

import com.campus.coupon.common.PageResponse;
import com.campus.coupon.template.dto.TemplateCreateRequest;
import com.campus.coupon.template.dto.TemplateResponse;

public interface CouponTemplateService {
    String createTemplate(TemplateCreateRequest request);

    TemplateResponse getByTemplateNo(String TemplateNo);

    PageResponse<TemplateResponse> pageList(Long current, Long size);
}
