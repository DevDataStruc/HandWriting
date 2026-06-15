package com.example.handwriting.common.result;

/**
 * 分页请求参数基类
 */
public class PageQuery {

    private long pageNum = 1L;
    private long pageSize = 10L;

    public long getPageNum() {
        return pageNum <= 0 ? 1L : pageNum;
    }

    public void setPageNum(long pageNum) {
        this.pageNum = pageNum;
    }

    public long getPageSize() {
        if (pageSize <= 0) return 10L;
        return Math.min(pageSize, 100L);
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }
}
