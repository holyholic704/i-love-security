package com.example.security.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.security.bean.ResponseResult;
import com.example.security.bean.User;
import com.example.security.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

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
            return ResponseResult.success("注册成功");
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
}
