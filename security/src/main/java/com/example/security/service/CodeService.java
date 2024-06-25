package com.example.security.service;

import cn.hutool.core.util.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class CodeService {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取一个验证码
     *
     * @param phone 手机号
     * @return 验证码
     */
    public String getCode(String phone) {
        // 模拟生成一个验证码
        String code = String.valueOf(RandomUtil.getRandom().nextInt(1000, 9999));
        // 将验证码放入缓存，有效时间5分钟
        redisTemplate.opsForValue().set(phone, code, 5, TimeUnit.MINUTES);
        return code;
    }
}
