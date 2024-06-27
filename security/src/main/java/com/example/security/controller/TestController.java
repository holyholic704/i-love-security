package com.example.security.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.*;
import com.example.common.bean.ResponseResult;
import com.example.common.bean.User;
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

    public static void main(String[] args) {
        RSA rsa = new RSA(AsymmetricAlgorithm.RSA.getValue());
        String privateKey = rsa.getPrivateKeyBase64();
        String publicKey = rsa.getPublicKeyBase64();

        String s = "q=%E6%95%B0%E5%AD%97%E7%AD%BE%E5%90%8D+%E7%AE%97%E6%B3%95&sca_esv=de28fabd33e10b5e&ei=X3N6ZtuVKeK3vr0Pu6ivkQY&oq=%E6%95%B0%E5%AD%97%E7%AD%BE%E5%90%8D+&gs_lp";
        byte[] arr = StrUtil.utf8Bytes(s);

        String encrypt = SecureUtil.rsa(null, publicKey).encryptBase64(arr, KeyType.PublicKey);
        System.out.println(encrypt);

        String decrypt =  SecureUtil.rsa(privateKey, null).decryptStr(encrypt, KeyType.PrivateKey);
        System.out.println(decrypt);


        String salt = "jiage";
        System.out.println(SecureUtil.md5().digestHex(s + salt));
        System.out.println(SecureUtil.md5().digestHex(s));


        byte[] data = "我是一段测试字符串".getBytes();
        Sign sign = SecureUtil.sign(SignAlgorithm.SHA256withRSA);
        byte[] signed = sign.sign(data);
        boolean verify = sign.verify(data, signed);
        System.out.println(Base64.encode(signed));
    }
}
