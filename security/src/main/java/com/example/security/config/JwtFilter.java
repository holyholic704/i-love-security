package com.example.security.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.security.bean.ResponseResult;
import com.example.security.bean.User;
import com.example.security.service.RolePermissionService;
import com.example.security.service.UserRoleService;
import com.example.security.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import java.util.*;

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

//                if (!this.check(response, jwtUtil.getUserIdFromToken(token), request.getRequestURI())) {
//                    return;
//                }
                // 添加权限列表
                user.setAuthorities(this.getAuthorities(user.getId()));

                // 认证
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    @Autowired
    private RolePermissionService rolePermissionService;
    @Autowired
    private UserRoleService userRoleService;

    /**
     * 权限判断
     */
    private boolean check(HttpServletResponse response, Long userId, String url) throws IOException {
        Map<Long, List<Long>> userRole = userRoleService.getUserRole();
        List<Long> roleIds;

        if (MapUtil.isNotEmpty(userRole) && userRole.containsKey(userId) && CollUtil.isNotEmpty(roleIds = userRole.get(userId))) {
            Map<Long, Set<String>> rolePermission = rolePermissionService.getRolePermission();
            if (MapUtil.isNotEmpty(rolePermission)) {
                for (Number roleId : roleIds) {
                    roleId = roleId.longValue();
                    if (rolePermission.containsKey(roleId)) {
                        Collection<String> set = rolePermission.get(roleId);
                        if (set.contains(url)) {
                            return true;
                        }
                    }
                }
            }
        }

        renderJson(response, ResponseResult.error("你没有权限啊"));
        return false;
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

    /**
     * 获取权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    private Set<GrantedAuthority> getAuthorities(Long userId) {
        Map<Long, List<Long>> userRole = userRoleService.getUserRole();
        List<Long> roleIds;
        Set<GrantedAuthority> result = new HashSet<>();

        if (MapUtil.isNotEmpty(userRole) && userRole.containsKey(userId) && CollUtil.isNotEmpty(roleIds = userRole.get(userId))) {
            Map<Long, Set<String>> rolePermission = rolePermissionService.getRolePermission();
            if (MapUtil.isNotEmpty(rolePermission)) {
                for (Number roleId : roleIds) {
                    roleId = roleId.longValue();
                    if (rolePermission.containsKey(roleId)) {
                        Collection<String> permissions = rolePermission.get(roleId);
                        for (String permission : permissions) {
                            result.add(new SimpleGrantedAuthority(permission));
                        }
                    }
                }
            }
        }
        return result;
    }
}
