package com.campus.coupon.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.coupon.common.BusinessException;
import com.campus.coupon.coupon.entity.UserCoupon;
import com.campus.coupon.coupon.mapper.UserCouponMapper;
import com.campus.coupon.order.constant.OrderStatus;
import com.campus.coupon.order.dto.OrderCreateRequest;
import com.campus.coupon.order.dto.OrderResponse;
import com.campus.coupon.order.entity.Order;
import com.campus.coupon.order.mapper.OrderMapper;
import com.campus.coupon.order.service.OrderService;
import com.campus.coupon.template.mapper.CouponTemplateMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Resource
    private OrderMapper orderMapper;

    @Resource
    private UserCouponMapper userCouponMapper;

    @Resource
    private CouponTemplateMapper couponTemplateMapper;

    @Resource
    private OrderTokenConsumer orderTokenConsumer;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public OrderResponse createOrder(OrderCreateRequest request){
        orderTokenConsumer.consumerOrThrow(request.getUserId(), request.getToken());

        UserCoupon coupon = userCouponMapper.selectOne(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getCouponNo, request.getCouponNo())
                        .eq(UserCoupon::getUserId, request.getUserId())
        );
        if(coupon == null){
            throw new BusinessException(3002, "优惠券不存在或不属于当前用户");
        }

        String orderNo = generateOrderNo();

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(request.getUserId());
        order.setTemplateId(coupon.getTemplateId());
        order.setCouponNo(request.getCouponNo());
        order.setAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setCancelledAt(null);

        try{
            transactionTemplate.executeWithoutResult(status ->{
                int updated = userCouponMapper.update(null,
                        new LambdaUpdateWrapper<UserCoupon>()
                                .eq(UserCoupon::getCouponNo, request.getCouponNo())
                                .eq(UserCoupon::getUserId, request.getUserId())
                                .eq(UserCoupon::getStatus, 0)
                                .set(UserCoupon::getStatus, 1)
                                .set(UserCoupon::getOrderNo, orderNo));
                if(updated != 1){
                    throw new BusinessException(3003, "优惠券已被使用或锁定");
                }
                orderMapper.insert(order);
            });
        }catch (DuplicateKeyException e){
            log.warn("订单号重复：orderNo={}", orderNo);
            throw new BusinessException(3001, "请勿重复提交");
        }

        return toResponse(order);
    }

    @Override
    public OrderResponse getByOrderNo(String OrderNo){
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, OrderNo)
        );
        if(order == null){
            throw new BusinessException(3004, "订单不存在");
        }
        return toResponse(order);
    }

    private OrderResponse toResponse(Order order){
        OrderResponse resp = new OrderResponse();
        resp.setOrderNo(order.getOrderNo());
        resp.setCouponNo(order.getCouponNo());
        resp.setAmount(order.getAmount());
        resp.setStatus(order.getStatus());
        resp.setCreatedAt(order.getCreatedAt());
        return resp;
    }

    private String generateOrderNo(){
        return "0" + System.currentTimeMillis()
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
