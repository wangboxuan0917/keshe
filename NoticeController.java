package com.keshe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keshe.common.PageResult;
import com.keshe.common.Result;
import com.keshe.common.SecurityUtil;
import com.keshe.dto.PageParam;
import com.keshe.entity.SysNotice;
import com.keshe.service.SysNoticeService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    @Resource
    private SysNoticeService noticeService;

    /** 分页查询（管理端） */
    @GetMapping("/page")
    public Result<PageResult<SysNotice>> page(PageParam param) {
        Page<SysNotice> page = noticeService.page(
                new Page<>(param.getPage(), param.getPageSize()),
                new LambdaQueryWrapper<SysNotice>()
                        .like(StringUtils.hasText(param.getKeyword()), SysNotice::getTitle, param.getKeyword())
                        .orderByDesc(SysNotice::getPublishTime, SysNotice::getCreateTime));
        return Result.success(PageResult.build(page));
    }

    /** 获取已发布的公告列表（首页用） */
    @GetMapping("/published")
    public Result<List<SysNotice>> published() {
        List<SysNotice> list = noticeService.list(
                new LambdaQueryWrapper<SysNotice>()
                        .eq(SysNotice::getStatus, 1)
                        .orderByDesc(SysNotice::getPublishTime)
                        .last("LIMIT 10"));
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<SysNotice> get(@PathVariable Long id) {
        return Result.success(noticeService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody SysNotice notice) {
        if (notice.getStatus() == 1) {
            notice.setPublishTime(LocalDateTime.now());
            notice.setPublisher(SecurityUtil.getCurrentUsername());
        }
        noticeService.save(notice);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SysNotice notice) {
        // 如果改为发布状态，设置发布时间
        if (notice.getStatus() == 1) {
            SysNotice old = noticeService.getById(notice.getId());
            if (old != null && old.getStatus() != 1) {
                notice.setPublishTime(LocalDateTime.now());
                notice.setPublisher(SecurityUtil.getCurrentUsername());
            }
        }
        noticeService.updateById(notice);
        return Result.success();
    }

    /** 发布公告 */
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        SysNotice notice = noticeService.getById(id);
        if (notice == null) return Result.error("公告不存在");
        notice.setStatus(1);
        notice.setPublishTime(LocalDateTime.now());
        notice.setPublisher(SecurityUtil.getCurrentUsername());
        noticeService.updateById(notice);
        return Result.success();
    }

    /** 下架公告 */
    @PostMapping("/{id}/unpublish")
    public Result<Void> unpublish(@PathVariable Long id) {
        SysNotice notice = noticeService.getById(id);
        if (notice == null) return Result.error("公告不存在");
        notice.setStatus(2);
        noticeService.updateById(notice);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.removeById(id);
        return Result.success();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Result.error("请选择要删除的公告");
        noticeService.removeByIds(ids);
        return Result.success();
    }
}
