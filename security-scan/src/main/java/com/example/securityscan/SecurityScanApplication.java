package com.example.securityscan;

import com.example.common.util.JwtUtil;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@MapperScan("com.example.*.mapper")
public class SecurityScanApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityScanApplication.class, args);
    }

    @Bean
    JwtUtil jwtUtil() {
        return new JwtUtil();
    }
}
