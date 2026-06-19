package com.keshe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.keshe.common.Result;
import com.keshe.entity.*;
import com.keshe.mapper.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/access/role-device")
public class RoleDeviceController {

    @Resource
    private SysRoleMapper roleMapper;
    @Resource
    private DeviceDoorMapper deviceMapper;
    @Resource
    private AccessRuleMapper ruleMapper;
    @Resource
    private AccessRuleDeviceMapper ruleDeviceMapper;

    private static final Long VISITOR_VIRTUAL_ID = -1L;

    @GetMapping
    public Result<Map<String, Object>> getMatrix() {
        List<SysRole> roles = new ArrayList<>(roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSort)));
        // 添加虚拟"访客"角色
        SysRole visitorRole = new SysRole();
        visitorRole.setId(VISITOR_VIRTUAL_ID);
        visitorRole.setName("访客");
        visitorRole.setCode("VISITOR");
        roles.add(visitorRole);

        List<DeviceDoor> devices = deviceMapper.selectList(new LambdaQueryWrapper<DeviceDoor>().orderByAsc(DeviceDoor::getId));

        List<AccessRule> rules = ruleMapper.selectList(
                new LambdaQueryWrapper<AccessRule>().eq(AccessRule::getStatus, 1));

        Map<Long, Long> roleToRuleMap = new HashMap<>();
        for (AccessRule r : rules) {
            if (r.getRoleId() != null) {
                roleToRuleMap.put(r.getRoleId(), r.getId());
            }
        }
        // 访客规则用-1标识
        AccessRule visitorAccessRule = rules.stream()
                .filter(r -> r.getRoleId() == null && "访客设备权限".equals(r.getName()))
                .findFirst().orElse(null);
        if (visitorAccessRule != null) {
            roleToRuleMap.put(VISITOR_VIRTUAL_ID, visitorAccessRule.getId());
        }

        List<AccessRuleDevice> links = ruleDeviceMapper.selectList(null);
        Map<Long, Set<Long>> ruleDeviceMap = new HashMap<>();
        for (AccessRuleDevice rd : links) {
            ruleDeviceMap.computeIfAbsent(rd.getRuleId(), k -> new HashSet<>()).add(rd.getDeviceId());
        }

        Set<String> allowedPairs = new HashSet<>();
        for (Map.Entry<Long, Long> e : roleToRuleMap.entrySet()) {
            Long roleId = e.getKey();
            Long ruleId = e.getValue();
            Set<Long> deviceIds = ruleDeviceMap.getOrDefault(ruleId, new HashSet<>());
            for (Long deviceId : deviceIds) {
                allowedPairs.add(roleId + ":" + deviceId);
            }
        }
        // 超级管理员和管理员显示所有设备
        for (SysRole role : roleMapper.selectList(null)) {
            if ("SUPER_ADMIN".equals(role.getCode()) || "ADMIN".equals(role.getCode())) {
                for (DeviceDoor d : devices) {
                    allowedPairs.add(role.getId() + ":" + d.getId());
                }
            }
        }

        return Result.success(Map.of("roles", roles, "devices", devices, "allowedPairs", allowedPairs));
    }

    @PostMapping
    public Result<Void> saveMatrix(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> allowedPairs = (List<String>) body.get("allowedPairs");

        List<SysRole> roles = roleMapper.selectList(null);
        Map<Long, SysRole> roleMap = roles.stream().collect(Collectors.toMap(SysRole::getId, r -> r));

        Map<Long, Set<Long>> roleDevices = new HashMap<>();
        Set<Long> visitorDeviceIds = new HashSet<>();
        if (allowedPairs != null) {
            for (String pair : allowedPairs) {
                String[] parts = pair.split(":");
                Long roleId = Long.parseLong(parts[0]);
                Long deviceId = Long.parseLong(parts[1]);
                if (roleId.equals(VISITOR_VIRTUAL_ID)) {
                    visitorDeviceIds.add(deviceId);
                } else {
                    roleDevices.computeIfAbsent(roleId, k -> new HashSet<>()).add(deviceId);
                }
            }
        }

        // 自动为超级管理员和管理员添加所有设备权限
        List<SysRole> adminRoles = roles.stream()
                .filter(r -> "SUPER_ADMIN".equals(r.getCode()) || "ADMIN".equals(r.getCode()))
                .collect(Collectors.toList());
        List<DeviceDoor> allDevices = deviceMapper.selectList(null);
        for (SysRole adminRole : adminRoles) {
            Set<Long> adminDeviceIds = roleDevices.computeIfAbsent(adminRole.getId(), k -> new HashSet<>());
            for (DeviceDoor d : allDevices) adminDeviceIds.add(d.getId());
        }

        // 处理每个角色的规则
        for (Map.Entry<Long, Set<Long>> e : roleDevices.entrySet()) {
            Long roleId = e.getKey();
            Set<Long> deviceIds = e.getValue();
            if (deviceIds.isEmpty()) continue;
            SysRole role = roleMap.get(roleId);
            if (role == null) continue;

            AccessRule rule = ruleMapper.selectOne(
                    new LambdaQueryWrapper<AccessRule>()
                            .eq(AccessRule::getRoleId, roleId)
                            .eq(AccessRule::getStatus, 1)
                            .last("LIMIT 1"));
            if (rule == null) {
                rule = new AccessRule();
                rule.setName(role.getName() + "设备权限");
                rule.setRoleId(roleId);
                rule.setTimeType(4);
                rule.setWeekDays("1,2,3,4,5,6,7");
                rule.setStatus(1);
                rule.setPriority(0);
                ruleMapper.insert(rule);
            } else if (rule.getWeekDays() == null || rule.getWeekDays().isEmpty()) {
                rule.setWeekDays("1,2,3,4,5,6,7");
                ruleMapper.updateById(rule);
            }
            ruleDeviceMapper.delete(new LambdaQueryWrapper<AccessRuleDevice>().eq(AccessRuleDevice::getRuleId, rule.getId()));
            for (Long deviceId : deviceIds) {
                ruleDeviceMapper.insert(new AccessRuleDevice(null, rule.getId(), deviceId));
            }
        }

        // 处理访客权限
        AccessRule visitorRule = ruleMapper.selectOne(
                new LambdaQueryWrapper<AccessRule>().eq(AccessRule::getName, "访客设备权限").eq(AccessRule::getStatus, 1).last("LIMIT 1"));
        if (!visitorDeviceIds.isEmpty()) {
            if (visitorRule == null) {
                visitorRule = new AccessRule();
                visitorRule.setName("访客设备权限");
                visitorRule.setRoleId(null);
                visitorRule.setTimeType(4);
                visitorRule.setWeekDays("1,2,3,4,5,6,7");
                visitorRule.setStatus(1);
                visitorRule.setPriority(0);
                ruleMapper.insert(visitorRule);
            } else if (visitorRule.getWeekDays() == null || visitorRule.getWeekDays().isEmpty()) {
                visitorRule.setWeekDays("1,2,3,4,5,6,7");
                ruleMapper.updateById(visitorRule);
            }
            ruleDeviceMapper.delete(new LambdaQueryWrapper<AccessRuleDevice>().eq(AccessRuleDevice::getRuleId, visitorRule.getId()));
            for (Long deviceId : visitorDeviceIds) {
                ruleDeviceMapper.insert(new AccessRuleDevice(null, visitorRule.getId(), deviceId));
            }
        } else if (visitorRule != null) {
            // 没有访客权限则删除旧规则
            ruleDeviceMapper.delete(new LambdaQueryWrapper<AccessRuleDevice>().eq(AccessRuleDevice::getRuleId, visitorRule.getId()));
            ruleMapper.deleteById(visitorRule.getId());
        }

        // 清理未被处理的非管理员角色规则
        Set<Long> processedRoleIds = roleDevices.keySet();
        List<AccessRule> allRoleRules = ruleMapper.selectList(
                new LambdaQueryWrapper<AccessRule>().isNotNull(AccessRule::getRoleId).eq(AccessRule::getStatus, 1));
        for (AccessRule r : allRoleRules) {
            SysRole role = roleMap.get(r.getRoleId());
            if (role != null && ("SUPER_ADMIN".equals(role.getCode()) || "ADMIN".equals(role.getCode()))) continue;
            if (!processedRoleIds.contains(r.getRoleId())) {
                ruleDeviceMapper.delete(new LambdaQueryWrapper<AccessRuleDevice>().eq(AccessRuleDevice::getRuleId, r.getId()));
                ruleMapper.deleteById(r.getId());
            }
        }

        return Result.success();
    }
}
