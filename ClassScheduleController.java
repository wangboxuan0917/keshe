package com.keshe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keshe.common.PageResult;
import com.keshe.common.Result;
import com.keshe.dto.PageParam;
import com.keshe.entity.ClassSchedule;
import com.keshe.service.ClassScheduleService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class ClassScheduleController {

    @Resource
    private ClassScheduleService scheduleService;

    @GetMapping("/page")
    public Result<PageResult<ClassSchedule>> page(PageParam param) {
        Page<ClassSchedule> page = scheduleService.page(
                new Page<>(param.getPage(), param.getPageSize()),
                new LambdaQueryWrapper<ClassSchedule>()
                        .like(StringUtils.hasText(param.getKeyword()), ClassSchedule::getCourseName, param.getKeyword())
                        .orderByAsc(ClassSchedule::getDayOfWeek, ClassSchedule::getStartTime));
        return Result.success(PageResult.build(page));
    }

    @GetMapping("/list")
    public Result<List<ClassSchedule>> list() {
        return Result.success(scheduleService.list());
    }

    @GetMapping("/{id}")
    public Result<ClassSchedule> get(@PathVariable Long id) {
        return Result.success(scheduleService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody ClassSchedule schedule) {
        scheduleService.save(schedule);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody ClassSchedule schedule) {
        scheduleService.updateById(schedule);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scheduleService.removeById(id);
        return Result.success();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Result.error("请选择要删除的课程");
        scheduleService.removeByIds(ids);
        return Result.success();
    }
}
