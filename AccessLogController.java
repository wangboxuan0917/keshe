package com.keshe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keshe.common.PageResult;
import com.keshe.common.Result;
import com.keshe.dto.PageParam;
import com.keshe.entity.AccessLog;
import com.keshe.service.AccessLogService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/access/log")
public class AccessLogController {

    @Resource
    private AccessLogService accessLogService;

    @GetMapping("/page")
    public Result<PageResult<AccessLog>> page(PageParam param) {
        Page<AccessLog> page = accessLogService.page(
                new Page<>(param.getPage(), param.getPageSize()),
                new LambdaQueryWrapper<AccessLog>()
                        .like(StringUtils.hasText(param.getKeyword()), AccessLog::getUserName, param.getKeyword())
                        .orderByDesc(AccessLog::getAccessTime));
        return Result.success(PageResult.build(page));
    }

    @GetMapping("/list")
    public Result<List<AccessLog>> list() {
        return Result.success(accessLogService.list());
    }

    /** 获取最近出入记录（首页用） */
    @GetMapping("/recent")
    public Result<List<AccessLog>> recent() {
        Page<AccessLog> page = accessLogService.page(
                new Page<>(1, 10),
                new LambdaQueryWrapper<AccessLog>().orderByDesc(AccessLog::getAccessTime));
        return Result.success(page.getRecords());
    }

    /** 出入统计 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        List<AccessLog> all = accessLogService.list();
        long success = all.stream().filter(l -> l.getResult() == 1).count();
        long denied = all.stream().filter(l -> l.getResult() == 2).count();
        long today = all.stream().filter(l -> l.getAccessTime() != null &&
                l.getAccessTime().toLocalDate().equals(java.time.LocalDate.now())).count();
        return Result.success(Map.of("total", all.size(), "success", success,
                "denied", denied, "today", today));
    }
}
