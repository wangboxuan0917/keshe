package com.keshe.dto;

import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 分页请求参数
 */
@Data
public class PageParam {
    @Min(value = 1, message = "页码最小为1")
    private int page = 1;

    @Min(value = 1, message = "每页大小最小为1")
    private int pageSize = 10;

    private String keyword;
    private String startDate;
    private String endDate;
}
