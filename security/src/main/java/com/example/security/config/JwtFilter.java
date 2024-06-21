package com.example.security.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.security.bean.ResponseResult;
import com.example.security.bean.User;
import com.example.security.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Value("${jwt.header}")
    private String header;
    @Value("${jwt.suffiex}")
    private String suffiex;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 获取请求头里携带的token
        String token = request.getHeader(header);
        if (StrUtil.isNotEmpty(token) && StrUtil.startWith(token, suffiex)) {
            token = token.substring(suffiex.length());

            // 通过token获取用户信息，用于随后的校验
            User user = jwtUtil.getUserFromToken(token);

            if (user != null && jwtUtil.check(token, user)) {
                // 判断缓存中是否存在该token
                if (!redisTemplate.hasKey(user.getUsername())) {
                    renderJson(response, ResponseResult.error("请重新登录"));
                    return;
                }

                // 认证
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 用于渲染JSON
     */
    public static void renderJson(HttpServletResponse response, ResponseResult data) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        ServletOutputStream out = response.getOutputStream();
        String str = JSONUtil.toJsonStr(data);
        out.write(str.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }
}
