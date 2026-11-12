package com.campus.coupon.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e){
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e){
        log.warn("唯一索引冲突：{}", e.getMessage());
        return Result.fail(1002, "数据已存在，请勿重复提交");
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e){
        String message = e.getBindingResult().getFieldErrors().stream().findFirst()
                .map(org.springframework.validation.FieldError::getDefaultMessage)
                .orElse("参数校验失败");
        return Result.fail(1003, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e){
        log.error("系统异常", e);
        return Result.fail(500, "系统繁忙，请稍后再试");
    }
}
