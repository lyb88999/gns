package com.gns.notification.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class PageResult<T> {

    private List<T> content;
    private long totalElements;
    private long totalPages;

    public PageResult() {
    }

    public PageResult(List<T> content, long totalElements, long totalPages) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

}
