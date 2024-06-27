package com.example.securityscan.config;

import com.example.securityscan.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    // TODO 注意
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // TODO 注意
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors()
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf().disable()
                .authorizeRequests()
                .antMatchers("/getQrUrl", "/checkUrl", "/allowLogin")
                .permitAll()
                .anyRequest()
                .authenticated();
    }

    @Bean
    ScanAuthenticationProvider scanAuthenticationProvider() {
        return new ScanAuthenticationProvider();
    }

    @Autowired
    UserService userService;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // 如果实现了多个UserDetailsService，需指定哪个Provider用的是哪个UserDetailsService
        auth.authenticationProvider(scanAuthenticationProvider()).userDetailsService(userService);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 允许访问的源
        corsConfiguration.addAllowedOriginPattern("*");
        // 是否需要携带身份凭证
        corsConfiguration.setAllowCredentials(true);
        // 预检请求的响应结果能够缓存多久
        corsConfiguration.setMaxAge(86400L);
        // 允许携带的请求头
        corsConfiguration.setAllowedHeaders(Collections.singletonList("*"));
        // 允许使用的请求方法
        corsConfiguration.setAllowedMethods(Collections.singletonList("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
