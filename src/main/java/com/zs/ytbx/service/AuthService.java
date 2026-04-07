package com.zs.ytbx.service;

import com.zs.ytbx.dto.*;

public interface AuthService {
    String login(LoginRequest request);
    void register(RegisterRequest request);
    Long getCurrentUserId();
    String getCurrentUsername();
    boolean isAdmin();
}
