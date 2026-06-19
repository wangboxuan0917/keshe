package com.keshe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keshe.common.PageResult;
import com.keshe.common.Result;
import com.keshe.dto.PageParam;
import com.keshe.entity.AttendanceRecord;
import com.keshe.service.AttendanceRecordService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/attendance/record")
public class AttendanceRecordController {

    @Resource
    private AttendanceRecordService recordService;

    @GetMapping("/page")
    public Result<PageResult<AttendanceRecord>> page(PageParam param) {
        Page<AttendanceRecord> page = recordService.page(
                new Page<>(param.getPage(), param.getPageSize()),
                new LambdaQueryWrapper<AttendanceRecord>()
                        .like(StringUtils.hasText(param.getKeyword()), AttendanceRecord::getUserName, param.getKeyword())
                        .orderByDesc(AttendanceRecord::getDate));
        return Result.success(PageResult.build(page));
    }

    @GetMapping("/list")
    public Result<List<AttendanceRecord>> list() {
        return Result.success(recordService.list());
    }

    @GetMapping("/{id}")
    public Result<AttendanceRecord> get(@PathVariable Long id) {
        return Result.success(recordService.getById(id));
    }

    @PutMapping
    public Result<Void> update(@RequestBody AttendanceRecord record) {
        recordService.updateById(record);
        return Result.success();
    }
}
