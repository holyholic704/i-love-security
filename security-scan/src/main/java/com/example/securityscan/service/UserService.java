package com.example.securityscan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.bean.User;
import com.example.common.mapper.UserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class UserService extends ServiceImpl<UserMapper, User> implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        Assert.isTrue(user != null, "当前用户不存在");
        return user;
    }
}
