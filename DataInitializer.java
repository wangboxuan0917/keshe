package com.keshe.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.keshe.entity.*;
import com.keshe.mapper.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    @Resource
    private SysUserMapper userMapper;
    @Resource
    private SysRoleMapper roleMapper;
    @Resource
    private SysPermissionMapper permissionMapper;
    @Resource
    private SysUserRoleMapper userRoleMapper;
    @Resource
    private SysRolePermissionMapper rolePermissionMapper;
    @Resource
    private AttendanceRuleMapper attendanceRuleMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        SysUser admin = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin"));
        if (admin == null) {
            System.out.println(">>> 数据初始化: 创建默认数据...");
            initAll();
        } else {
            // 保证admin密码正确
            if (!passwordEncoder.matches("123456", admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode("123456"));
                userMapper.updateById(admin);
            }
            // 保证admin用户名为admin（防止被导入覆盖）
            if (!"admin".equals(admin.getUsername())) {
                admin.setUsername("admin");
                userMapper.updateById(admin);
            }
            // 每次启动同步权限
            System.out.println(">>> 数据初始化: 同步权限数据...");
            syncPermissions();
        }
        // 初始化明文密码缓存（导入的用户默认密码都是123456）
        java.util.List<com.keshe.entity.SysUser> allUsers = userMapper.selectList(null);
        java.util.Map<Long, String> pwdCache = new java.util.HashMap<>();
        pwdCache.put(1L, "123456");
        for (com.keshe.entity.SysUser u : allUsers) {
            if (u.getId() != 1L) pwdCache.put(u.getId(), "123456");
        }
        com.keshe.controller.SysUserController.initPlainPasswordCache(pwdCache);

        System.out.println(">>> 数据初始化完成");
    }

    /** 全新初始化 */
    private void initAll() {
        String encodedPwd = passwordEncoder.encode("123456");
        SysUser admin = new SysUser();
        admin.setUsername("admin"); admin.setPassword(encodedPwd); admin.setRealName("系统管理员");
        admin.setPhone("13800000000"); admin.setStatus(1); admin.setType(1);
        userMapper.insert(admin);

        SysRole superRole = new SysRole(); superRole.setName("超级管理员"); superRole.setCode("SUPER_ADMIN"); superRole.setSort(1); superRole.setStatus(1); roleMapper.insert(superRole);
        SysRole adminRole = new SysRole(); adminRole.setName("管理员"); adminRole.setCode("ADMIN"); adminRole.setSort(2); adminRole.setStatus(1); roleMapper.insert(adminRole);
        SysRole studentRole = new SysRole(); studentRole.setName("学生"); studentRole.setCode("STUDENT"); studentRole.setSort(3); studentRole.setStatus(1); roleMapper.insert(studentRole);

        // 插入权限
        syncPermissions();

        // admin分配超级管理员角色
        userRoleMapper.insert(new SysUserRole(null, admin.getId(), superRole.getId()));

        // 超级管理员拥有所有权限
        List<SysPermission> allPerms = permissionMapper.selectList(null);
        allPerms.forEach(p -> rolePermissionMapper.insert(new SysRolePermission(null, superRole.getId(), p.getId())));

        AttendanceRule rule = new AttendanceRule();
        rule.setName("默认考勤规则"); rule.setCheckInStart(java.time.LocalTime.of(8, 0));
        rule.setCheckInEnd(java.time.LocalTime.of(9, 0)); rule.setCheckInLateEnd(java.time.LocalTime.of(12, 0));
        rule.setCheckOutStart(java.time.LocalTime.of(17, 0)); rule.setCheckOutEnd(java.time.LocalTime.of(18, 0));
        rule.setStatus(1); attendanceRuleMapper.insert(rule);
    }

    /** 同步权限：保证所有已知权限项都存在，并分配给超级管理员角色 */
    private void syncPermissions() {
        // 1. 获取所有已有权限（按permission标识索引）
        java.util.Map<String, SysPermission> existing = new HashMap<>();
        for (SysPermission p : permissionMapper.selectList(null)) {
            existing.put(p.getPermission(), p);
        }

        // 2. 定义所有权限项（按顺序，父子关系通过parentPermission标识）
        // 格式: [name, permission标识, type, parentPermission, path, icon, sort]
        String[][] allPermDefs = {
            // 系统管理
            {"系统管理", "system", "1", null, "/system", "Setting", "1"},
            {"用户管理", "system:user:list", "1", "system", "/system/user", "User", "1"},
            {"新增用户", "system:user:add", "2", "system:user:list", null, null, "1"},
            {"编辑用户", "system:user:edit", "2", "system:user:list", null, null, "2"},
            {"删除用户", "system:user:delete", "2", "system:user:list", null, null, "3"},
            {"角色管理", "system:role:list", "1", "system", "/system/role", "Safety", "2"},
            {"权限管理", "system:perm:list", "1", "system", "/system/permission", "Lock", "3"},
            {"通知公告", "system:notice:list", "1", "system", "/system/notice", "Bell", "4"},
            // 门禁管理
            {"门禁管理", "device", "1", null, "/device", "Monitor", "2"},
            {"设备管理", "device:door:list", "1", "device", "/device/door", "Cpu", "1"},
            {"新增设备", "device:door:add", "2", "device:door:list", null, null, "1"},
            {"编辑设备", "device:door:edit", "2", "device:door:list", null, null, "2"},
            {"删除设备", "device:door:delete", "2", "device:door:list", null, null, "3"},
            // 通行管理
            {"通行管理", "access", "1", null, "/access", "Key", "3"},
            {"通行规则", "access:rule:list", "1", "access", "/access/rule", "Finished", "1"},
            {"出入记录", "access:log:list", "1", "access", "/access/log", "Document", "2"},
            {"刷卡测试", "access:card-swipe:list", "1", "access", "/access/card-swipe", "CreditCard", "3"},
            // 访客管理
            {"访客管理", "visitor", "1", null, "/visitor", "Avatar", "4"},
            {"访客信息", "visitor:info:list", "1", "visitor", "/visitor/info", "InfoFilled", "1"},
            {"访客记录", "visitor:record:list", "1", "visitor", "/visitor/record", "List", "2"},
            {"访客审批", "visitor:approve", "2", "visitor:record:list", null, null, "1"},
            // 考勤管理
            {"考勤管理", "attendance", "1", null, "/attendance", "Calendar", "5"},
            {"考勤规则", "attendance:rule:list", "1", "attendance", "/attendance/rule", "Setting", "1"},
            {"考勤记录", "attendance:record:list", "1", "attendance", "/attendance/record", "Reading", "2"},
            {"考勤统计", "attendance:stats", "1", "attendance", "/attendance/stats", "DataAnalysis", "3"},
            {"课程表", "attendance:schedule:list", "1", "attendance", "/attendance/schedule", "Reading", "4"},
            // 报表统计
            {"报表统计", "report", "1", null, "/report", "TrendCharts", "6"},
            {"出入报表", "report:access", "1", "report", "/report/access", "Document", "1"},
            {"考勤报表", "report:attendance", "1", "report", "/report/attendance", "Calendar", "2"},
        };

        // 3. 逐个处理：缺失则插入，已存在则修正parentId
        java.util.Map<String, Long> permIdMap = new HashMap<>();
        for (String[] def : allPermDefs) {
            String permKey = def[1];
            if (existing.containsKey(permKey)) {
                SysPermission p = existing.get(permKey);
                permIdMap.put(permKey, p.getId());
                // 修正parentId（旧数据可能用了硬编码ID）
                String parentKey = def[3];
                Long correctParentId = null;
                if (parentKey != null && permIdMap.containsKey(parentKey)) {
                    correctParentId = permIdMap.get(parentKey);
                }
                if (correctParentId != null && !correctParentId.equals(p.getParentId())) {
                    p.setParentId(correctParentId);
                    permissionMapper.updateById(p);
                }
                continue;
            }
            // 插入新权限
            SysPermission p = new SysPermission();
            p.setName(def[0]);
            p.setPermission(def[1]);
            p.setType(Integer.parseInt(def[2]));
            String parentKey = def[3];
            if (parentKey != null && permIdMap.containsKey(parentKey)) {
                p.setParentId(permIdMap.get(parentKey));
            }
            p.setPath(def[4]); p.setIcon(def[5]);
            p.setSort(Integer.parseInt(def[6])); p.setStatus(1);
            permissionMapper.insert(p);
            permIdMap.put(permKey, p.getId());
            existing.put(permKey, p);
        }

        // 4. 超级管理员角色确保拥有所有权限
        SysRole superRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "SUPER_ADMIN"));
        if (superRole != null) {
            List<Long> existingRolePermIds = rolePermissionMapper.selectList(
                    new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, superRole.getId()))
                    .stream().map(SysRolePermission::getPermissionId).collect(java.util.stream.Collectors.toList());

            for (SysPermission p : permissionMapper.selectList(null)) {
                if (!existingRolePermIds.contains(p.getId())) {
                    rolePermissionMapper.insert(new SysRolePermission(null, superRole.getId(), p.getId()));
                }
            }
        }
    }
}
