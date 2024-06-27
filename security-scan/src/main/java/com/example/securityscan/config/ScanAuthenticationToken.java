package com.example.securityscan.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class ScanAuthenticationToken extends AbstractAuthenticationToken {

    private Object principal;

    public ScanAuthenticationToken(Object principal) {
        super(null);
        this.principal = principal;
        super.setAuthenticated(false);
    }

    public ScanAuthenticationToken(Collection<? extends GrantedAuthority> authorities, Object principal) {
        super(authorities);
        this.principal = principal;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }
}
