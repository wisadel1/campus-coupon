package com.campus.coupon.order.service;

import com.campus.coupon.order.dto.OrderCreateRequest;
import com.campus.coupon.order.dto.OrderResponse;

public interface OrderService {
    OrderResponse createOrder(OrderCreateRequest request);
    OrderResponse getByOrderNo(String orderNo);
    boolean cancelOrder(String orderNo, String reason);
    String handlePayCallback(String orderNo);
}
