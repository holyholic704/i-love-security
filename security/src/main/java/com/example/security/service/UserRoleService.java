package com.example.security.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.bean.UserRole;
import com.example.common.mapper.UserRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserRoleService extends ServiceImpl<UserRoleMapper, UserRole> {

    @Autowired
    private RedisTemplate redisTemplate;

    private static final String USER_ROLE = "USER_ROLE";

    /**
     * 获取用户角色，并加入进缓存
     */
    public Map<Long, List<Long>> getUserRole() {
        if (redisTemplate.hasKey(USER_ROLE)) {
            return (Map<Long, List<Long>>) redisTemplate.opsForValue().get(USER_ROLE);
        }

        List<UserRole> list = this.list();

        Map<Long, List<Long>> map = new HashMap<>();

        if (CollUtil.isNotEmpty(list)) {
            for (UserRole userRole : list) {
                List<Long> set = map.computeIfAbsent(userRole.getUserId(), v -> new ArrayList<>());
                set.add(userRole.getRoleId());
            }
        }

        redisTemplate.opsForValue().set(USER_ROLE, map);

        return map;
    }
}
