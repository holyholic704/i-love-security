package com.example.security.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.security.bean.ResponseResult;
import com.example.security.bean.ResponseUser;
import com.example.security.bean.User;
import com.example.security.mapper.UserMapper;
import com.example.security.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

@Service
public class UserService extends ServiceImpl<UserMapper, User> implements UserDetailsService {

    @Value("${jwt.header}")
    private String header;
    @Value("${jwt.suffiex}")
    private String suffiex;
    @Value("${jwt.expired}")
    private Long expired;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 注册
     *
     * @param user 用户信息
     * @return 注册结果
     */
    public ResponseResult register(User user) {
        String username;
        String password;
        if (user != null && StrUtil.isNotEmpty(username = user.getUsername()) && StrUtil.isNotEmpty(password = user.getPassword()) && this.notExists(username)) {
            User register = new User()
                    .setUsername(username)
                    // 加密
                    .setPassword(bCryptPasswordEncoder.encode(password));
            this.save(register);

            // 认证
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // 将认证信息存储在SecurityContextHolder中
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 获取认证信息，也就是我们实现UserDetails的类
            User principal = (User) authentication.getPrincipal();
            // 生成token
            String token = jwtUtil.generate(principal);
            // 缓存token
            redisTemplate.opsForValue().set(username, token, expired, TimeUnit.SECONDS);

            return ResponseResult.success(new ResponseUser(user, token));
        }
        return ResponseResult.error("注册失败");
    }

    /**
     * 判断是否存在该用户
     *
     * @param username 用户名
     * @return 是否存在
     */
    private boolean notExists(String username) {
        return this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)) < 1;
    }

    /**
     * 根据用户名加载用户信息
     * 当然传入的可不止用户名，只要通过这个参数能获取到用户信息即可，例如手机号，身份证号等
     *
     * @param username 用户名
     * @return 用户信息，用于之后的认证授权
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        Assert.isTrue(user != null, "当前用户不存在");
        return user;
    }
}
