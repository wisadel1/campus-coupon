package com.campus.coupon.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.coupon.order.constant.OrderStatus;
import com.campus.coupon.order.entity.Order;
import com.campus.coupon.order.mapper.OrderMapper;
import com.campus.coupon.order.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderTimeoutTask {
    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderService orderService;

    @Scheduled(fixedDelay = 60_000)
    public void scanTimeoutOrders(){
        while(true) {
            List<Order> timeoutOrders = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getStatus, OrderStatus.CREATED)
                            .lt(Order::getExpireTime, LocalDateTime.now())
                            .last("LIMIT 100")
            );

            if(timeoutOrders.isEmpty()){
                break;
            }

            log.info("扫描到{}笔超时订单", timeoutOrders.size());
            for(Order order : timeoutOrders){
                try{
                    boolean cancelled = orderService.cancelOrder(order.getOrderNo(), "TIMEOUT");
                    log.info("超时关单：orderNo={}, cancelled={}", order.getOrderNo(), cancelled);
                }catch (Exception e){
                    log.error("超时关单失败：orderNo={}", order.getOrderNo(), e);
                }
            }
        }
    }
}
