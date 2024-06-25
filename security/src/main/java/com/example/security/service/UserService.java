package com.example.security.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.security.bean.ResponseResult;
import com.example.security.bean.ResponseUser;
import com.example.security.bean.User;
import com.example.security.config.PhoneAuthenticationToken;
import com.example.security.mapper.UserMapper;
import com.example.security.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Service
public class UserService extends ServiceImpl<UserMapper, User> implements UserDetailsService {

    @Value("${jwt.header}")
    private String header;
    @Value("${jwt.suffiex}")
    private String suffiex;
    @Value("${jwt.expired}")
    private Long expired;

    private static final String prefix = "PHONE";

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 登录
     *
     * @param user 用户信息
     * @return 登录结果
     */
    public ResponseResult login(User user) {
        String username;
        String password;
        if (user != null && StrUtil.isNotEmpty(username = user.getUsername())) {
            if (StrUtil.isNotEmpty(user.getCode())) {
                return this.loginPhone(user);
            }

            if (StrUtil.isNotEmpty(password = user.getPassword())) {
                ResponseUser responseUser = this.authenticate(username, password);
                return responseUser == null ? ResponseResult.error("用户名或密码错误") : ResponseResult.success(responseUser);
            }
        }
        return ResponseResult.error("登录失败");
    }

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

            return ResponseResult.success(this.authenticate(username, password));
        }
        return ResponseResult.error("注册失败");
    }

    /**
     * 退出登录
     *
     * @param request 请求
     */
    public void logout(HttpServletRequest request) {
        // 获取请求头里携带的token
        String token = request.getHeader(header);
        if (StrUtil.isNotEmpty(token) && StrUtil.startWith(token, suffiex)) {
            token = token.substring(suffiex.length());
            // 删除token缓存
            String username = jwtUtil.getUsernameFromToken(token);
            redisTemplate.delete(username);
            // 修改认证信息
            SecurityContextHolder.getContext().getAuthentication().setAuthenticated(false);
        }
    }

    /**
     * 认证
     *
     * @param username 用户名
     * @param password 密码
     * @return 用户信息及token
     */
    private ResponseUser authenticate(String username, String password) {
        // 认证
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (AuthenticationException e) {
            // 一般只有登录时认证失败才会进入这里
            return null;
        }

        // 将认证信息存储在SecurityContextHolder中
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 获取认证信息，也就是我们实现UserDetails的类
        User principal = (User) authentication.getPrincipal();
        // 生成token
        String token = jwtUtil.generate(principal);
        // 缓存token
        redisTemplate.opsForValue().set(username, token, expired, TimeUnit.SECONDS);

        return new ResponseUser(principal, token);
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

    /**
     * 手机登录
     *
     * @param user 用户信息
     * @return 是否登录成功
     */
    public ResponseResult loginPhone(User user) {
        String username = user.getUsername();
        String code = user.getCode();
        // 验证码校验
        if (this.checkCode(username, code)) {
            // 认证
            PhoneAuthenticationToken authenticationToken = new PhoneAuthenticationToken(username);
            Authentication authentication;
            try {
                authentication = authenticationManager.authenticate(authenticationToken);
            } catch (AuthenticationException e) {
                return ResponseResult.error("登录失败");
            }

            // 将认证信息存储在SecurityContextHolder中
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 获取手机号
            String principal = (String) authentication.getPrincipal();
            // 获取用户ID
            Long id = (Long) authentication.getDetails();
            // 生成token
            String token = jwtUtil.generate(user = new User(id, principal));
            // 加入缓存
            redisTemplate.opsForValue().set(username, token, expired, TimeUnit.SECONDS);
            return ResponseResult.success(new ResponseUser(user, token));
        }
        return ResponseResult.error("登录失败");
    }

    /**
     * 验证码校验
     *
     * @param phoneNum    手机号
     * @param receiveCode 收到的验证码
     * @return 是否校验成功
     */
    private boolean checkCode(String phoneNum, String receiveCode) {
        if (StrUtil.isNotBlank(phoneNum) && StrUtil.isNotBlank(receiveCode)) {
            String code = (String) redisTemplate.opsForValue().get(prefix + phoneNum);
            if (StrUtil.isNotBlank(code)) {
                return code.equals(receiveCode);
            }
        }
        return false;
    }
}
