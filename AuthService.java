package com.keshe.service;

import com.keshe.dto.LoginDTO;
import com.keshe.dto.LoginResultVO;

public interface AuthService {
    LoginResultVO login(LoginDTO dto);
    void logout();
}
