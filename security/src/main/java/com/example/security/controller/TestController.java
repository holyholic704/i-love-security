package com.example.security.controller;

import com.example.security.bean.ResponseResult;
import com.example.security.bean.User;
import com.example.security.service.CodeService;
import com.example.security.service.TestService;
import com.example.security.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
public class TestController {

    @Autowired
    private UserService userService;
    @Autowired
    private TestService testService;
    @Autowired
    private CodeService codeService;

    @CrossOrigin
    @GetMapping("test")
    public String test() {
        testService.test1();
        testService.test2();
        testService.test3();
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

    @GetMapping("getCode")
    public String getCode(@RequestParam String phone) {
        return codeService.getCode(phone);
    }
}
