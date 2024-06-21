package com.example.security.controller;

import com.example.security.bean.ResponseResult;
import com.example.security.bean.User;
import com.example.security.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class TestController {

    @Autowired
    private UserService userService;

    @GetMapping("test")
    public String test() {
        return "good";
    }

    @PostMapping("register")
    public ResponseResult register(@RequestBody User user) {
        return userService.register(user);
    }

    @PostMapping("login")
    public ResponseResult login(@RequestBody User user) {
        return userService.login(user);
    }

    @PostMapping("logout")
    public String logout(HttpServletRequest request) {
        userService.logout(request);
        return "done";
    }
}
