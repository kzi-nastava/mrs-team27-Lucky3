package com.example.mobile.models;

import java.util.List;

/**
 * Generic paginated response wrapper.
 * Matches Spring Data Page structure from backend.
 */
public class PageResponse<T> {
    
    private List<T> content;
    private Integer totalElements;
    private Integer totalPages;
    private Integer number; // current page number (0-indexed)
    private Integer size;   // page size
    
    public PageResponse() {
    }
    
    public List<T> getContent() {
        return content;
    }
    
    public void setContent(List<T> content) {
        this.content = content;
    }
    
    public Integer getTotalElements() {
        return totalElements;
    }
    
    public void setTotalElements(Integer totalElements) {
        this.totalElements = totalElements;
    }
    
    public Integer getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }
    
    public Integer getNumber() {
        return number;
    }
    
    public void setNumber(Integer number) {
        this.number = number;
    }
    
    public Integer getSize() {
        return size;
    }
    
    public void setSize(Integer size) {
        this.size = size;
    }
}
