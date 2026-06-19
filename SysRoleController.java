package com.keshe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keshe.common.PageResult;
import com.keshe.common.Result;
import com.keshe.dto.PageParam;
import com.keshe.entity.*;
import com.keshe.mapper.SysRolePermissionMapper;
import com.keshe.service.SysRoleService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/role")
public class SysRoleController {

    @Resource
    private SysRoleService roleService;
    @Resource
    private SysRolePermissionMapper rolePermissionMapper;

    @GetMapping("/page")
    public Result<PageResult<SysRole>> page(PageParam param) {
        Page<SysRole> page = roleService.page(
                new Page<>(param.getPage(), param.getPageSize()),
                new LambdaQueryWrapper<SysRole>()
                        .like(StringUtils.hasText(param.getKeyword()), SysRole::getName, param.getKeyword())
                        .orderByAsc(SysRole::getSort));
        return Result.success(PageResult.build(page));
    }

    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        return Result.success(roleService.list());
    }

    @PostMapping
    public Result<Void> add(@RequestBody SysRole role) {
        roleService.save(role);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SysRole role) {
        roleService.updateById(role);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.removeById(id);
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        return Result.success();
    }

    /** 获取角色的权限ID列表 */
    @GetMapping("/{id}/permissions")
    public Result<List<Long>> getRolePermissions(@PathVariable Long id) {
        List<Long> permIds = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id))
                .stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
        return Result.success(permIds);
    }

    /** 分配角色权限 */
    @PostMapping("/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        permissionIds.forEach(pid -> rolePermissionMapper.insert(new SysRolePermission(null, id, pid)));
        return Result.success();
    }
}
