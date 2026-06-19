package com.keshe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keshe.common.PageResult;
import com.keshe.common.Result;
import com.keshe.dto.PageParam;
import com.keshe.entity.AccessRule;
import com.keshe.entity.AccessRuleDevice;
import com.keshe.mapper.AccessRuleDeviceMapper;
import com.keshe.service.AccessRuleService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/access/rule")
public class AccessRuleController {

    @Resource
    private AccessRuleService ruleService;
    @Resource
    private AccessRuleDeviceMapper ruleDeviceMapper;

    @GetMapping("/page")
    public Result<PageResult<AccessRule>> page(PageParam param) {
        Page<AccessRule> page = ruleService.page(
                new Page<>(param.getPage(), param.getPageSize()),
                new LambdaQueryWrapper<AccessRule>()
                        .like(StringUtils.hasText(param.getKeyword()), AccessRule::getName, param.getKeyword())
                        .orderByDesc(AccessRule::getPriority));
        return Result.success(PageResult.build(page));
    }

    @GetMapping("/{id}")
    public Result<AccessRule> get(@PathVariable Long id) {
        return Result.success(ruleService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody AccessRule rule) {
        ruleService.save(rule);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody AccessRule rule) {
        ruleService.updateById(rule);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ruleService.removeById(id);
        ruleDeviceMapper.delete(new LambdaQueryWrapper<AccessRuleDevice>().eq(AccessRuleDevice::getRuleId, id));
        return Result.success();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Result.error("请选择要删除的规则");
        ruleService.removeByIds(ids);
        return Result.success();
    }

    /** 获取规则关联的设备ID */
    @GetMapping("/{id}/devices")
    public Result<List<Long>> getRuleDevices(@PathVariable Long id) {
        List<Long> deviceIds = ruleDeviceMapper.selectList(
                new LambdaQueryWrapper<AccessRuleDevice>().eq(AccessRuleDevice::getRuleId, id))
                .stream().map(AccessRuleDevice::getDeviceId).collect(Collectors.toList());
        return Result.success(deviceIds);
    }

    /** 分配规则设备 */
    @PostMapping("/{id}/devices")
    public Result<Void> assignDevices(@PathVariable Long id, @RequestBody List<Long> deviceIds) {
        ruleDeviceMapper.delete(new LambdaQueryWrapper<AccessRuleDevice>().eq(AccessRuleDevice::getRuleId, id));
        deviceIds.forEach(did -> ruleDeviceMapper.insert(new AccessRuleDevice(null, id, did)));
        return Result.success();
    }
}
