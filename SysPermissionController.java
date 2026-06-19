package com.keshe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.keshe.common.Result;
import com.keshe.entity.SysPermission;
import com.keshe.service.SysPermissionService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/permission")
public class SysPermissionController {

    @Resource
    private SysPermissionService permissionService;

    /** 获取树形权限列表 */
    @GetMapping("/tree")
    public Result<List<SysPermission>> tree() {
        List<SysPermission> all = permissionService.list(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSort));
        // 构建树
        List<SysPermission> roots = all.stream().filter(p -> p.getParentId() == null).collect(Collectors.toList());
        for (SysPermission root : roots) {
            buildTree(root, all);
        }
        return Result.success(roots);
    }

    private void buildTree(SysPermission parent, List<SysPermission> all) {
        List<SysPermission> children = all.stream()
                .filter(p -> parent.getId().equals(p.getParentId()))
                .collect(Collectors.toList());
        if (!children.isEmpty()) {
            // 使用transient方式传递children
            parent.setChildren(children);
            children.forEach(c -> buildTree(c, all));
        }
    }

    @GetMapping("/list")
    public Result<List<SysPermission>> list() {
        return Result.success(permissionService.list());
    }

    @PostMapping
    public Result<Void> add(@RequestBody SysPermission permission) {
        permissionService.save(permission);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SysPermission permission) {
        permissionService.updateById(permission);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        permissionService.removeById(id);
        return Result.success();
    }
}
