package com.campus.coupon.template.constant;

public final class TemplateRedisKeys {
    private TemplateRedisKeys(){}

    public static String detailKey(String templateNo){
        return "template:detail:" + templateNo;
    }
}
