package com.keshe.controller;

import com.keshe.common.Result;
import com.keshe.common.SecurityUtil;
import com.keshe.dto.PasswordDTO;
import com.keshe.entity.SysUser;
import com.keshe.service.SysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Resource
    private SysUserService userService;
    @Resource
    private PasswordEncoder passwordEncoder;

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return Result.unauthorized("未登录");

        SysUser user = userService.getById(userId);
        if (user == null) return Result.error("用户不存在");

        // 校验原密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            return Result.error("原密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userService.updateById(user);
        return Result.success("密码修改成功，请重新登录", null);
    }

    /** 更新个人信息 */
    @PutMapping("/info")
    public Result<Void> updateInfo(@RequestBody SysUser updateUser) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return Result.unauthorized("未登录");

        SysUser user = new SysUser();
        user.setId(userId);
        user.setRealName(updateUser.getRealName());
        user.setPhone(updateUser.getPhone());
        user.setEmail(updateUser.getEmail());
        user.setAvatar(updateUser.getAvatar());
        userService.updateById(user);
        return Result.success();
    }
}
