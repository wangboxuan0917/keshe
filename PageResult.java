package com.keshe.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回结果
 */
@Data
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private long total;       // 总记录数
    private long pageSize;    // 每页大小
    private long currentPage; // 当前页
    private long totalPages;  // 总页数
    private List<T> records;  // 数据列表

    public static <T> PageResult<T> build(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setPageSize(page.getSize());
        result.setCurrentPage(page.getCurrent());
        result.setTotalPages(page.getPages());
        result.setRecords(page.getRecords());
        return result;
    }
}
