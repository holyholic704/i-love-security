package com.example.securityscan.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.bean.ResponseResult;
import com.example.common.bean.ResponseUser;
import com.example.common.bean.User;
import com.example.common.util.JwtUtil;
import com.example.securityscan.config.ScanAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ScanService {

    @Value("${jwt.expired}")
    private Long expired;

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private JwtUtil jwtUtil;

    public ResponseResult getQrUrl() {
        String uuid = IdUtil.simpleUUID();
        redisTemplate.opsForValue().set(uuid, uuid, 2, TimeUnit.MINUTES);
        return ResponseResult.success(uuid);
    }

    public ResponseResult checkUrl(String url) {
        String result;
        if (StrUtil.isNotEmpty(url) && StrUtil.isNotEmpty(result = (String) redisTemplate.opsForValue().get(url))) {
            return ResponseResult.success(!url.equals(result));
        }
        return ResponseResult.error("请刷新二维码");
    }

    public ResponseResult allowLogin(String url, String username) {
        if (StrUtil.isNotEmpty(url) && StrUtil.isNotEmpty(username) && redisTemplate.hasKey(url)) {
            ScanAuthenticationToken authenticationToken = new ScanAuthenticationToken(username);
            Authentication authentication;
            try {
                authentication = authenticationManager.authenticate(authenticationToken);
            } catch (AuthenticationException e) {
                redisTemplate.delete(url);
                return ResponseResult.error("登录失败");
            }

            // 将认证信息存储在SecurityContextHolder中
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 获取手机号
            String principal = (String) authentication.getPrincipal();
            // 获取用户ID
            Long id = (Long) authentication.getDetails();
            // 生成token
            User user = new User(id, principal);
            String token = jwtUtil.generate(user);
            // 加入缓存
            redisTemplate.opsForValue().set(username, token, expired, TimeUnit.SECONDS);
            // 前端跳转
            redisTemplate.opsForValue().set(url, token, 5, TimeUnit.MINUTES);
            return ResponseResult.success(new ResponseUser(user, token));
        }
        return ResponseResult.error("请刷新二维码");
    }
}
