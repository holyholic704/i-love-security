package com.example.securityscan.config;

import com.example.common.bean.User;
import com.example.securityscan.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class ScanAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private UserService userService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ScanAuthenticationToken token = (ScanAuthenticationToken) authentication;
        String username = (String) token.getPrincipal();
        User user = (User) userService.loadUserByUsername(username);
        token.setAuthenticated(true);
        token.setDetails(user.getId());
        return authentication;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(ScanAuthenticationToken.class);
    }

}
