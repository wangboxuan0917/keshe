package com.keshe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录返回结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultVO {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private Integer userType;
    private List<String> permissions;
}
