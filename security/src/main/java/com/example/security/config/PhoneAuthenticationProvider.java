package com.example.security.config;

import com.example.common.bean.User;
import com.example.security.service.PhoneLoginService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class PhoneAuthenticationProvider implements AuthenticationProvider {

    private final PhoneLoginService phoneLoginService;

    public PhoneAuthenticationProvider(PhoneLoginService phoneLoginService) {
        this.phoneLoginService = phoneLoginService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        PhoneAuthenticationToken token = (PhoneAuthenticationToken) authentication;
        // 获取手机号
        String phoneNum = (String) token.getPrincipal();
        // 根据手机号获取用户信息
        User user = (User) phoneLoginService.loadUserByUsername(phoneNum);
        token.setAuthenticated(true);
        token.setDetails(user.getId());
        return authentication;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(PhoneAuthenticationToken.class);
    }

}
