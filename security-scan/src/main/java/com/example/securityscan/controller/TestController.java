package com.example.securityscan.controller;

import com.example.common.bean.ResponseResult;
import com.example.securityscan.service.ScanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private ScanService scanService;

    @GetMapping("getQrUrl")
    public ResponseResult getQrUrl() {
        return scanService.getQrUrl();
    }

    @GetMapping("checkUrl")
    public ResponseResult checkUrl(@RequestParam String url) {
        return scanService.checkUrl(url);
    }

    @GetMapping("allowLogin")
    public ResponseResult allowLogin(@RequestParam String url, @RequestParam String username) {
        return scanService.allowLogin(url, username);
    }
}
