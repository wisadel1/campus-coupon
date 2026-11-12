package com.campus.coupon.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> {
    private Long total;
    private Long current;
    private Long size;
    private List<T> records;

    public static <T> PageResponse<T> of(Long total, Long current, Long size, List<T> records){
        PageResponse<T> p = new PageResponse<>();
        p.setTotal(total);
        p.setCurrent(current);
        p.setSize(size);
        p.setRecords(records);
        return p;
    }
}
