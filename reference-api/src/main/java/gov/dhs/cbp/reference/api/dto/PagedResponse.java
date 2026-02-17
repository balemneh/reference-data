package gov.dhs.cbp.reference.api.dto;

import java.util.List;

public class PagedResponse<T> {
    
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private String selfLink;
    private String nextLink;
    private String prevLink;
    
    public PagedResponse() {}
    
    public PagedResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / size);
    }
    
    public List<T> getContent() {
        return content;
    }
    
    public void setContent(List<T> content) {
        this.content = content;
    }
    
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
    
    public long getTotalElements() {
        return totalElements;
    }
    
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    public String getSelfLink() {
        return selfLink;
    }
    
    public void setSelfLink(String selfLink) {
        this.selfLink = selfLink;
    }
    
    public String getNextLink() {
        return nextLink;
    }
    
    public void setNextLink(String nextLink) {
        this.nextLink = nextLink;
    }
    
    public String getPrevLink() {
        return prevLink;
    }
    
    public void setPrevLink(String prevLink) {
        this.prevLink = prevLink;
    }
    
    public boolean isFirst() {
        return page == 0;
    }
    
    public boolean isLast() {
        return page >= totalPages - 1;
    }

    public int getPageNumber() {
        return page;
    }

    public void setPageNumber(int pageNumber) {
        this.page = pageNumber;
    }

    public int getPageSize() {
        return size;
    }

    public void setPageSize(int pageSize) {
        this.size = pageSize;
    }
}