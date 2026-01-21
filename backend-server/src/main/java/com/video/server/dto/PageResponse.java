package com.video.server.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResponse<T> {
    private List<T> list;
    private Long total;
    private Integer page;
    private Integer pageSize;

    public PageResponse(List<T> list, Long total, Integer page, Integer pageSize) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    // 【新增】静态工厂方法
    public static <T> PageResponse<T> of(List<T> list, Long total, Integer page, Integer pageSize) {
        return new PageResponse<>(list, total, page, pageSize);
    }
}