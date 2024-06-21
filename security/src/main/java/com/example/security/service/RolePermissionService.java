package com.example.security.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.security.bean.RolePermission;
import com.example.security.mapper.RolePermissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RolePermissionService extends ServiceImpl<RolePermissionMapper, RolePermission> {

    @Autowired
    private RedisTemplate redisTemplate;

    private static final String ROLE_PERMISSION = "ROLE_PERMISSION";

    /**
     * 获取角色权限，并加入到缓存
     */
    public Map<Long, Set<String>> getRolePermission() {
        if (redisTemplate.hasKey(ROLE_PERMISSION)) {
            return (Map<Long, Set<String>>) redisTemplate.opsForValue().get(ROLE_PERMISSION);
        }

        List<RolePermission> list = this.getBaseMapper().getAll();

        Map<Long, Set<String>> map = new HashMap<>();

        if (CollUtil.isNotEmpty(list)) {
            for (RolePermission rolePermission : list) {
                Set<String> set = map.computeIfAbsent(rolePermission.getRoleId(), v -> new HashSet<>());
                set.add(rolePermission.getPermission());
            }
        }

        redisTemplate.opsForValue().set(ROLE_PERMISSION, map);

        return map;
    }
}
