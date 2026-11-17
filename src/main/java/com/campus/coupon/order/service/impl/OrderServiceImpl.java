package com.campus.coupon.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.coupon.common.BusinessException;
import com.campus.coupon.coupon.constant.CouponRedisKeys;
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
import org.springframework.data.redis.connection.SortParameters;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    @Resource
    private StringRedisTemplate stringRedisTemplate;

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
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));

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

    @Override
    public boolean cancelOrder(String orderNo, String reason){
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
        );
        if(order == null){
            throw new BusinessException(3004, "订单不存在");
        }

        boolean cancelled = transactionTemplate.execute(status ->{
            int rows = orderMapper.update(null,
                    new LambdaUpdateWrapper<Order>()
                            .eq(Order::getOrderNo, orderNo)
                            .eq(Order::getStatus, OrderStatus.CREATED)
                            .set(Order::getStatus, OrderStatus.CANCELLED)
                            .set(Order::getCancelledAt, LocalDateTime.now()));
            if(rows != 1){
                log.info("订单[{}]状态不是CREATED，取消动作放弃", orderNo);
                return false;
            }

            int couponRows = userCouponMapper.update(null,
                    new LambdaUpdateWrapper<UserCoupon>()
                            .eq(UserCoupon::getOrderNo, orderNo)
                            .eq(UserCoupon::getStatus, 1)
                            .set(UserCoupon::getStatus, 0)
                            .set(UserCoupon::getOrderNo, null));

            if(couponRows != 1){
                log.error("订单[{}]取消成功但回滚失败，需人工核对", orderNo);
            }

            return true;
        });

        return cancelled;
    }

    // TODO: 这里不能简单的将cancelled订单设置为paid，需要根据订单支付时间与过期时间进行评判，如果支付晚于过期则退款
    @Override
    public String handlePayCallback(String orderNo){
        int rows = orderMapper.update(null,
                new LambdaUpdateWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .in(Order::getStatus, OrderStatus.CREATED, OrderStatus.CANCELLED)
                        .set(Order::getStatus, OrderStatus.PAID)
                        .set(Order::getPaidAt, LocalDateTime.now()));
        if(rows == 1){
            return "PAID";
        }
    // TODO: 为了防止同一订单被多次支付，需要设置状态锁，引入processing
        Order current = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
        );
        if(current == null){
            throw new BusinessException(3004, "订单不存在");
        }
        if(OrderStatus.PAID.equals(current.getStatus())){
            log.info("重复支付回调，订单[{}]已支付", orderNo);
            return "PAID";
        }
        return "REFUND_REQUIRED";
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
