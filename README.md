# Spring Security 上手

- [仓库地址](https://github.com/holyholic704/i-love-security)

## 项目搭建

首先搭建一个 Spring Boot 项目，并添加一个接口

> 这一步不需要多说了吧

```java
@RestController
public class TestController {

    @GetMapping("test")
    public String test() {
        return "good";
    }
}
```

> 这一步不需要多说了吧

## 引入 Spring Security 依赖

因为 Spring Boot 遵循了约定优于配置，当你引入了该依赖时，Spring Security 已经起作用了

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

这时再请求上面的接口，你会发现跳转到一个登录页面，这个就说明 Spring Security 已经起作用了

- 默认账号：user
- 默认密码：每次项目启动时会在控制台打印出来

当然也可以自己设定密码，但没啥必要，不实用，正常开发中也不会使用

## 编写配置类

放开 `/test` 接口，无需登录即可访问

```java
@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 授权
        // 所有请求都需认证
        // test接口无需验证
        http.authorizeRequests()
                .antMatchers("/test")
                .permitAll()
                .anyRequest()
                .authenticated();
    }
}
```

## 用户登录认证



