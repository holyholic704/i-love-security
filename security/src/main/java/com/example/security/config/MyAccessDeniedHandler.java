package com.example.security.config;

import cn.hutool.json.JSONUtil;
import com.example.security.bean.ResponseCode;
import com.example.security.bean.ResponseResult;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class MyAccessDeniedHandler implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        renderJson(response, ResponseResult.code(ResponseCode.AUTH_ERROR));
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
