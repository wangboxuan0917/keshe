package com.keshe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keshe.common.PageResult;
import com.keshe.common.Result;
import com.keshe.dto.PageParam;
import com.keshe.entity.Visitor;
import com.keshe.service.VisitorService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/visitor/info")
public class VisitorController {

    @Resource
    private VisitorService visitorService;
    @Resource
    private com.keshe.mapper.VisitorMapper visitorMapper;

    @GetMapping("/page")
    public Result<PageResult<Visitor>> page(PageParam param) {
        Page<Visitor> page = visitorService.page(
                new Page<>(param.getPage(), param.getPageSize()),
                new LambdaQueryWrapper<Visitor>()
                        .like(StringUtils.hasText(param.getKeyword()), Visitor::getName, param.getKeyword())
                        .or()
                        .like(StringUtils.hasText(param.getKeyword()), Visitor::getPhone, param.getKeyword())
                        .orderByDesc(Visitor::getCreateTime));
        return Result.success(PageResult.build(page));
    }

    @GetMapping("/{id}")
    public Result<Visitor> get(@PathVariable Long id) {
        return Result.success(visitorService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Visitor visitor) {
        visitor.setVisitCount(0);
        // 自动生成IC卡号（为空时）：前8位当天日期 + 后3位随机
        if (visitor.getCardNo() == null || visitor.getCardNo().trim().isEmpty()) {
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Random rand = new Random();
            String cardNo;
            do {
                String suffix = String.format("%03d", rand.nextInt(1000));
                cardNo = datePart + suffix;
            } while (visitorMapper.selectOne(
                    new LambdaQueryWrapper<com.keshe.entity.Visitor>().eq(com.keshe.entity.Visitor::getCardNo, cardNo)) != null);
            visitor.setCardNo(cardNo);
        }
        visitorService.save(visitor);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Visitor visitor) {
        visitorService.updateById(visitor);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        visitorService.removeById(id);
        return Result.success();
    }

    /** 切换黑名单 */
    @PostMapping("/{id}/blacklist")
    public Result<Void> toggleBlacklist(@PathVariable Long id, @RequestBody Visitor body) {
        Visitor visitor = visitorService.getById(id);
        if (visitor != null) {
            visitor.setBlacklist(body.getBlacklist());
            visitorService.updateById(visitor);
        }
        return Result.success();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Result.error("请选择要删除的访客");
        visitorService.removeByIds(ids);
        return Result.success();
    }
}
