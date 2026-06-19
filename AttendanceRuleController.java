package com.keshe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.keshe.common.Result;
import com.keshe.entity.AttendanceRule;
import com.keshe.service.AttendanceRuleService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/attendance/rule")
public class AttendanceRuleController {

    @Resource
    private AttendanceRuleService ruleService;

    @GetMapping("/list")
    public Result<List<AttendanceRule>> list() {
        return Result.success(ruleService.list());
    }

    @GetMapping("/{id}")
    public Result<AttendanceRule> get(@PathVariable Long id) {
        return Result.success(ruleService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody AttendanceRule rule) {
        ruleService.save(rule);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody AttendanceRule rule) {
        ruleService.updateById(rule);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ruleService.removeById(id);
        return Result.success();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Result.error("请选择要删除的规则");
        ruleService.removeByIds(ids);
        return Result.success();
    }
}
