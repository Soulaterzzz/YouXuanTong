package com.zs.ytbx.service;

import com.zs.ytbx.common.auth.SessionUser;
import com.zs.ytbx.dto.*;

public interface AuthService {
    SessionUser login(LoginRequest request);
    void register(RegisterRequest request);
}
