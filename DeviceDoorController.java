package com.keshe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keshe.common.PageResult;
import com.keshe.common.Result;
import com.keshe.dto.PageParam;
import com.keshe.entity.DeviceDoor;
import com.keshe.service.DeviceDoorService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/device/door")
public class DeviceDoorController {

    @Resource
    private DeviceDoorService deviceDoorService;

    @GetMapping("/page")
    public Result<PageResult<DeviceDoor>> page(PageParam param) {
        Page<DeviceDoor> page = deviceDoorService.page(
                new Page<>(param.getPage(), param.getPageSize()),
                new LambdaQueryWrapper<DeviceDoor>()
                        .like(StringUtils.hasText(param.getKeyword()), DeviceDoor::getName, param.getKeyword())
                        .or()
                        .like(StringUtils.hasText(param.getKeyword()), DeviceDoor::getCode, param.getKeyword())
                        .orderByDesc(DeviceDoor::getCreateTime));
        return Result.success(PageResult.build(page));
    }

    @GetMapping("/list")
    public Result<List<DeviceDoor>> list() {
        return Result.success(deviceDoorService.list());
    }

    @GetMapping("/{id}")
    public Result<DeviceDoor> get(@PathVariable Long id) {
        return Result.success(deviceDoorService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody DeviceDoor device) {
        deviceDoorService.save(device);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody DeviceDoor device) {
        deviceDoorService.updateById(device);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deviceDoorService.removeById(id);
        return Result.success();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Result.error("请选择要删除的设备");
        deviceDoorService.removeByIds(ids);
        return Result.success();
    }

    /** 远程控制门 */
    @PostMapping("/{id}/control")
    public Result<Void> control(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        DeviceDoor device = deviceDoorService.getById(id);
        if (device == null) return Result.error("设备不存在");
        String action = (String) body.get("action");
        if ("open".equals(action)) {
            device.setDoorStatus(1);
        } else if ("close".equals(action)) {
            device.setDoorStatus(0);
        } else if ("lock".equals(action)) {
            device.setStatus(0);
        } else if ("unlock".equals(action)) {
            device.setStatus(1);
        }
        deviceDoorService.updateById(device);
        return Result.success();
    }

    /** 获取设备统计 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        List<DeviceDoor> all = deviceDoorService.list();
        long online = all.stream().filter(d -> d.getStatus() == 1).count();
        long offline = all.stream().filter(d -> d.getStatus() == 0).count();
        long fault = all.stream().filter(d -> d.getStatus() == 2).count();
        long open = all.stream().filter(d -> d.getDoorStatus() == 1).count();
        return Result.success(Map.of(
                "total", all.size(), "online", online,
                "offline", offline, "fault", fault, "open", open));
    }
}
