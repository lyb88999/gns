package com.gns.notification.dto;

import java.util.List;

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

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }
}
