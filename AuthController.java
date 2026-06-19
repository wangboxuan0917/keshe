package com.keshe.controller;

import com.keshe.common.Result;
import com.keshe.common.SecurityUtil;
import com.keshe.dto.LoginDTO;
import com.keshe.dto.LoginResultVO;
import com.keshe.entity.SysUser;
import com.keshe.service.AuthService;
import com.keshe.service.SysUserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;
    @Resource
    private SysUserService userService;

    @PostMapping("/login")
    public Result<LoginResultVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @GetMapping("/me")
    public Result<SysUser> me() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return Result.unauthorized("未登录");
        SysUser user = userService.getById(userId);
        if (user != null) user.setPassword(null);
        return Result.success(user);
    }
}
