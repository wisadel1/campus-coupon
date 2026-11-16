package com.campus.coupon.order.controller;

import com.campus.coupon.common.Result;
import com.campus.coupon.order.dto.OrderCreateRequest;
import com.campus.coupon.order.dto.OrderResponse;
import com.campus.coupon.order.service.OrderService;
import com.campus.coupon.order.service.OrderTokenService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    @Resource
    private OrderTokenService orderTokenService;

    @Resource
    private OrderService orderService;

    @GetMapping("/token")
    public Result<String> token(@RequestParam Long userId){
        return Result.success(orderTokenService.generate(userId));
    }

    @PostMapping("/create")
    public Result<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request){
        return Result.success(orderService.createOrder(request));
    }

    @GetMapping("/{orderNo}")
    public Result<OrderResponse> detail(@PathVariable String orderNo){
        return Result.success(orderService.getByOrderNo(orderNo));
    }
}
