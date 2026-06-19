package com.keshe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keshe.common.PageResult;
import com.keshe.common.Result;
import com.keshe.dto.PageParam;
import com.keshe.entity.VisitorRecord;
import com.keshe.service.VisitorRecordService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/visitor/record")
public class VisitorRecordController {

    @Resource
    private VisitorRecordService recordService;

    @GetMapping("/page")
    public Result<PageResult<VisitorRecord>> page(PageParam param) {
        Page<VisitorRecord> page = recordService.page(
                new Page<>(param.getPage(), param.getPageSize()),
                new LambdaQueryWrapper<VisitorRecord>()
                        .like(StringUtils.hasText(param.getKeyword()), VisitorRecord::getVisitorName, param.getKeyword())
                        .orderByDesc(VisitorRecord::getCreateTime));
        return Result.success(PageResult.build(page));
    }

    @GetMapping("/{id}")
    public Result<VisitorRecord> get(@PathVariable Long id) {
        return Result.success(recordService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody VisitorRecord record) {
        record.setStatus(0); // 待审批
        recordService.save(record);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody VisitorRecord record) {
        recordService.updateById(record);
        return Result.success();
    }

    /** 审批 */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody VisitorRecord body) {
        VisitorRecord record = recordService.getById(id);
        if (record == null) return Result.error("记录不存在");
        record.setStatus(body.getStatus()); // 1批准 4拒绝
        recordService.updateById(record);
        return Result.success();
    }

    /** 签到 */
    @PostMapping("/{id}/checkin")
    public Result<Void> checkin(@PathVariable Long id) {
        VisitorRecord record = recordService.getById(id);
        if (record == null) return Result.error("记录不存在");
        record.setStatus(2);
        record.setActualStart(LocalDateTime.now());
        recordService.updateById(record);
        return Result.success();
    }

    /** 签离 */
    @PostMapping("/{id}/checkout")
    public Result<Void> checkout(@PathVariable Long id) {
        VisitorRecord record = recordService.getById(id);
        if (record == null) return Result.error("记录不存在");
        record.setStatus(3);
        record.setActualEnd(LocalDateTime.now());
        recordService.updateById(record);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        recordService.removeById(id);
        return Result.success();
    }
}
