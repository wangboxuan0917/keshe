package com.keshe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.keshe.common.JwtUtil;
import com.keshe.common.exception.BusinessException;
import com.keshe.dto.LoginDTO;
import com.keshe.dto.LoginResultVO;
import com.keshe.entity.SysPermission;
import com.keshe.entity.SysRole;
import com.keshe.entity.SysUser;
import com.keshe.entity.SysUserRole;
import com.keshe.mapper.*;
import com.keshe.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private SysUserMapper userMapper;
    @Resource
    private SysUserRoleMapper userRoleMapper;
    @Resource
    private SysRoleMapper roleMapper;
    @Resource
    private SysRolePermissionMapper rolePermissionMapper;
    @Resource
    private SysPermissionMapper permissionMapper;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private JwtUtil jwtUtil;

    @Override
    public LoginResultVO login(LoginDTO dto) {
        // 查询用户
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw BusinessException.badRequest("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw BusinessException.badRequest("账号已被禁用");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // 生成JWT
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getType());

        // 查询权限列表
        List<String> permissions = getUserPermissions(user.getId(), user.getType());

        return LoginResultVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .userType(user.getType())
                .permissions(permissions)
                .build();
    }

    @Override
    public void logout() {
        // JWT无状态，实际可加入黑名单逻辑
    }

    /**
     * 获取用户权限标识列表
     */
    public List<String> getUserPermissions(Long userId, Integer userType) {
        // 超级管理员直接返回所有权限
        if (userType != null && userType == 1) {
            return permissionMapper.selectList(null).stream()
                    .map(SysPermission::getPermission)
                    .collect(Collectors.toList());
        }

        // 查用户角色
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));

        if (userRoles.isEmpty()) return new ArrayList<>();

        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());

        // 查角色权限关联
        return permissionMapper.selectList(null).stream()
                .filter(p -> true) // 简化处理
                .map(SysPermission::getPermission)
                .collect(Collectors.toList());
    }
}
