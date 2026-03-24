package com.zs.ytbx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.dto.*;
import com.zs.ytbx.entity.AxxUserEntity;
import com.zs.ytbx.entity.AccountBalanceEntity;
import com.zs.ytbx.mapper.AxxUserMapper;
import com.zs.ytbx.mapper.AccountBalanceMapper;
import com.zs.ytbx.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AxxUserMapper axxUserMapper;
    private final AccountBalanceMapper accountBalanceMapper;

    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUsername = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> currentIsAdmin = new ThreadLocal<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String login(LoginRequest request) {
        LambdaQueryWrapper<AxxUserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AxxUserEntity::getUsername, request.getUsername())
               .eq(AxxUserEntity::getStatus, "ACTIVE")
               .eq(AxxUserEntity::getDeleted, 0);
        
        AxxUserEntity user = axxUserMapper.selectOne(wrapper);
        
        if (user == null) {
            throw new BusinessException(ResultCode.LOGIN_FAILED, "用户不存在或已被禁用");
        }
        
        if (!request.getPassword().equals(user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED, "用户名或密码错误");
        }
        
        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        axxUserMapper.updateById(user);
        
        currentUserId.set(user.getId());
        currentUsername.set(user.getUsername());
        currentIsAdmin.set("ADMIN".equals(user.getUserType()));
        
        return "ADMIN".equals(user.getUserType()) ? "ADMIN" : "USER";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        LambdaQueryWrapper<AxxUserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AxxUserEntity::getUsername, request.getUsername())
               .eq(AxxUserEntity::getDeleted, 0);
        
        if (axxUserMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
        }
        
        AxxUserEntity user = new AxxUserEntity();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setMobile(request.getMobile());
        user.setUserType("USER");
        user.setStatus("ACTIVE");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        axxUserMapper.insert(user);
        
        AccountBalanceEntity balance = new AccountBalanceEntity();
        balance.setUserId(user.getId());
        balance.setBalance(BigDecimal.ZERO);
        balance.setFrozenBalance(BigDecimal.ZERO);
        balance.setCreateTime(LocalDateTime.now());
        balance.setUpdateTime(LocalDateTime.now());
        accountBalanceMapper.insert(balance);
    }

    @Override
    public Long getCurrentUserId() {
        Long userId = currentUserId.get();
        return userId != null ? userId : 1L;
    }

    @Override
    public String getCurrentUsername() {
        String username = currentUsername.get();
        return username != null ? username : "guest";
    }

    @Override
    public boolean isAdmin() {
        Boolean isAdmin = currentIsAdmin.get();
        return isAdmin != null && isAdmin;
    }

    public void clear() {
        currentUserId.remove();
        currentUsername.remove();
        currentIsAdmin.remove();
    }
}
