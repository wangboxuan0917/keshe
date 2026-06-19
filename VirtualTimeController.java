package com.keshe.controller;

import com.keshe.common.Result;
import com.keshe.entity.SysConfig;
import com.keshe.mapper.SysConfigMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/system/config")
public class VirtualTimeController {

    @Resource
    private SysConfigMapper configMapper;

    @GetMapping("/virtual-time")
    public Result<Map<String, Object>> getVirtualTime() {
        SysConfig config = configMapper.selectById("virtual_time");
        String vt = (config != null && config.getConfigValue() != null && !config.getConfigValue().isEmpty())
                ? config.getConfigValue() : "";
        return Result.success(Map.of(
                "virtualTime", vt,
                "realTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "isActive", !vt.isEmpty()
        ));
    }

    @PostMapping("/virtual-time")
    public Result<Void> setVirtualTime(@RequestBody Map<String, String> body) {
        String virtualTime = body.getOrDefault("virtualTime", "");
        SysConfig config = configMapper.selectById("virtual_time");
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey("virtual_time");
            config.setRemark("调试用虚拟时间");
            config.setConfigValue(virtualTime);
            configMapper.insert(config);
        } else {
            config.setConfigValue(virtualTime);
            configMapper.updateById(config);
        }
        return Result.success();
    }
}
