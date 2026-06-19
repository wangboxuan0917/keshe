package com.keshe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keshe.common.PageResult;
import com.keshe.common.Result;
import com.keshe.dto.PageParam;
import com.keshe.entity.SysUser;
import com.keshe.entity.SysUserRole;
import com.keshe.mapper.SysUserRoleMapper;
import com.keshe.service.SysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/user")
public class SysUserController {

    @Resource
    private SysUserService userService;
    @Resource
    private SysUserRoleMapper userRoleMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    // 缓存明文密码（仅用于页面显示，重启后失效）
    private static final Map<Long, String> plainPasswordCache = new HashMap<>();

    @GetMapping("/page")
    public Result<PageResult<SysUser>> page(PageParam param) {
        Page<SysUser> page = userService.page(
                new Page<>(param.getPage(), param.getPageSize()),
                new LambdaQueryWrapper<SysUser>()
                        .like(StringUtils.hasText(param.getKeyword()), SysUser::getUsername, param.getKeyword())
                        .or()
                        .like(StringUtils.hasText(param.getKeyword()), SysUser::getRealName, param.getKeyword())
                        .orderByAsc(SysUser::getId)
        );
        page.getRecords().forEach(u -> {
            u.setPassword(null);
            u.setPlainPassword(plainPasswordCache.get(u.getId()));
        });
        return Result.success(PageResult.build(page));
    }

    @GetMapping("/list")
    public Result<List<SysUser>> list() {
        List<SysUser> list = userService.list();
        list.forEach(u -> {
            u.setPassword(null);
            u.setPlainPassword(plainPasswordCache.get(u.getId()));
        });
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<SysUser> get(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
            user.setPlainPassword(plainPasswordCache.get(id));
        }
        return Result.success(user);
    }

    @GetMapping("/{id}/roles")
    public Result<List<Long>> getUserRoles(@PathVariable Long id) {
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id))
                .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        return Result.success(roleIds);
    }

    @PostMapping
    public Result<Void> add(@RequestBody SysUser user) {
        String rawPwd = StringUtils.hasText(user.getPassword()) ? user.getPassword() : "123456";
        user.setPassword(passwordEncoder.encode(rawPwd));
        userService.save(user);
        plainPasswordCache.put(user.getId(), rawPwd);
        saveUserRoles(user.getId(), user.getRoleIds());
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SysUser user) {
        if (StringUtils.hasText(user.getPassword())) {
            String rawPwd = user.getPassword(); // 加密前保存明文
            user.setPassword(passwordEncoder.encode(rawPwd));
            plainPasswordCache.put(user.getId(), rawPwd);
        } else {
            user.setPassword(null);
        }
        userService.updateById(user);
        if (user.getRoleIds() != null) {
            saveUserRoles(user.getId(), user.getRoleIds());
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        plainPasswordCache.remove(id);
        return Result.success();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Result.error("请选择要删除的用户");
        userService.removeByIds(ids);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, ids));
        ids.forEach(plainPasswordCache::remove);
        return Result.success();
    }

    /** 批量重置密码为随机密码 */
    @PostMapping("/reset-passwords")
    public Result<List<SysUser>> resetPasswords() {
        List<SysUser> users = userService.list();
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        Random rand = new Random();
        List<SysUser> result = new ArrayList<>();

        for (SysUser user : users) {
            if ("admin".equals(user.getUsername())) {
                user.setPassword(null);
                user.setPlainPassword("123456");
                plainPasswordCache.put(user.getId(), "123456");
                result.add(user);
                continue;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(chars.charAt(rand.nextInt(chars.length())));
            }
            String plainPwd = sb.toString();
            user.setPassword(passwordEncoder.encode(plainPwd));
            userService.updateById(user);
            user.setPassword(null);
            user.setPlainPassword(plainPwd);
            plainPasswordCache.put(user.getId(), plainPwd);
            result.add(user);
        }
        return Result.success(result);
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds != null && !roleIds.isEmpty()) {
            roleIds.forEach(roleId -> userRoleMapper.insert(new SysUserRole(null, userId, roleId)));
        }
    }
    /** 初始化明文密码缓存（启动时由DataInitializer调用） */
    public static void initPlainPasswordCache(java.util.Map<Long, String> cache) {
        plainPasswordCache.putAll(cache);
    }

}
