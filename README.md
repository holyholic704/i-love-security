# Spring Security 简单上手

Spring Security 的简单使用，不包含理论知识，较为（特别）简陋，包括 Spring Security 引入，用户授权认证，JWT 的简单使用等

- [仓库地址](https://github.com/holyholic704/i-love-security)
- 涉及技术
  - Spring Boot：项目基础
  - Spring Security：主角
  - MySQL：存储用户、角色、权限
  - Redis：缓存，存储 JWT 等
  - Mybatis Plus
  - JWT
  - hutool：工具包
- 开发环境：基于 JDK1.8，因为如此项目中涉及到的各种依赖版本都不高

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

## 编写注册接口

可以拉取仓库的 preparation 分支，获取已配置好的 Mybatis、Redis、实体类等，用以接下来的操作，就不在在这一一列举了

创建 UserService 并编写注册方法

```java
/**
 * 注册
 *
 * @param user 用户信息
 * @return 注册结果
 */
public ResponseResult register(User user) {
    String username;
    String password;
    if (user != null && StrUtil.isNotEmpty(username = user.getUsername()) && StrUtil.isNotEmpty(password = user.getPassword()) && this.notExists(username)) {
        User register = new User()
                .setUsername(username)
                // 加密
                .setPassword(bCryptPasswordEncoder.encode(password));
        this.save(register);
        return ResponseResult.success("注册成功");
    }
    return ResponseResult.error("注册失败");
}

/**
 * 判断是否存在该用户
 *
 * @param username 用户名
 * @return 是否存在
 */
private boolean notExists(String username) {
    return this.count(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, username)) < 1;
}
```

记得在配置类中放开注册接口，并且配置好 BCryptPasswordEncoder，否则会报错

> Field bCryptPasswordEncoder in com.example.security.service.UserService required a bean of type 'org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder' that could not be found.

```java
@Bean
public BCryptPasswordEncoder bCryptPasswordEncoder() {
    return new BCryptPasswordEncoder();
}
```

如果放开接口仍报 403，可以尝试在配置类中关闭 CSRF 保护

```java
protected void configure(HttpSecurity http) throws Exception {
    // 关闭CSRF保护
    http.csrf().disable()
            .authorizeRequests()
            .antMatchers("/test", "/register")
            .permitAll()
            .anyRequest()
            .authenticated();
}
```

至此你就可以在数据中看到自己注册的用户了，而且密码是加密状态

## 整合 JWT 进行登录认证

首先引入 JWT 的依赖

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.9.1</version>
</dependency>
```

改造 User，实现 UserDetails 接口，也可新定义一个类实现

```java
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseModel implements UserDetails {

    private String username;

    private String password;

    private String realname;

    private Boolean del;

    public User(Long id, String username) {
        this.setId(id);
        this.username = username;
    }

    @TableField(exist = false)
    private Collection<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

定义一个关于用户信息的返回结果，隐藏一些敏感信息如密码等，也可直接在 User 类中定义

```java
@Data
public class ResponseUser {

    private Long id;

    private String username;

    private String token;

    public ResponseUser(User user, String token) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.token = token;
    }
}
```

编写 JWT 工具类

```java
@Component
public class JwtUtil {

    /**
     * 加密密钥
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * token有效期
     */
    @Value("${jwt.expired}")
    private Long expired;

    /**
     * 用户id
     */
    private final String CLAIMS_USER_ID = "user_id";

    /**
     * 生成token
     *
     * @param user 用户信息
     * @return token
     */
    public String generate(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIMS_USER_ID, user.getId());

        // 可以对照JWT的概念，了解这些字段的意思
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setExpiration(this.generateExpiration(expired))
                .setIssuedAt(new Date())
                .compressWith(CompressionCodecs.DEFLATE)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /**
     * 获取到期时间
     *
     * @param expiredTime 有效时间
     * @return 到期时间
     */
    private Date generateExpiration(long expiredTime) {
        return new Date(System.currentTimeMillis() + expiredTime * 1000);
    }

    /**
     * 验证token
     *
     * @param token token
     * @param user  用户信息
     * @return 是否通过认证
     */
    public boolean check(String token, User user) {
        return StrUtil.isNotEmpty(token) && user != null && this.checkUser(token, user);
    }

    /**
     * 验证token
     *
     * @param token token
     * @param user  用户信息
     * @return 是否通过认证
     */
    private boolean checkUser(String token, User user) {
        // 获取token中的信息
        Claims claims = this.getClaimsFromToken(token);
        // 校验
        return String.valueOf(user.getId()).equals(String.valueOf(claims.get(CLAIMS_USER_ID)))
                && user.getUsername().equals(claims.getSubject())
                && !isTokenExpired(token);
    }

    /**
     * 获取token中的信息
     *
     * @param token token
     * @return token中的信息
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(this.secret).parseClaimsJws(token).getBody();
    }

    /**
     * 判断token是否在有效期内
     *
     * @param token token
     * @return 是否在有效期内
     */
    public boolean isTokenExpired(String token) {
        return this.getExpirationFromToken(token).before(new Date());
    }

    /**
     * 获取token中的到期时间
     *
     * @param token token
     * @return 到期时间
     */
    public Date getExpirationFromToken(String token) {
        return this.getClaimsFromToken(token).getExpiration();
    }

    /**
     * 获取token中的用户名
     *
     * @param token token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return this.getClaimsFromToken(token).getSubject();
    }

    /**
     * 获取token中的用户ID
     *
     * @param token token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(String.valueOf(this.getClaimsFromToken(token).get(CLAIMS_USER_ID)));
    }

    /**
     * 获取token中的用户信息
     *
     * @param token token
     * @return 用户信息
     */
    public User getUserFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        long userId = Long.parseLong(String.valueOf(claims.get(CLAIMS_USER_ID)));
        return new User(userId, claims.getSubject());
    }
}
```

在配置文件添加 JWT 的相关信息

```yaml
jwt:
  header: Authorization
  suffiex: Bearer
  expired: 86400
  secret: jiage
```

改造 UserService

```java
@Service
public class UserService extends ServiceImpl<UserMapper, User> implements UserDetailsService {

    @Value("${jwt.header}")
    private String header;
    @Value("${jwt.suffiex}")
    private String suffiex;
    @Value("${jwt.expired}")
    private Long expired;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 登录
     *
     * @param user 用户信息
     * @return 登录结果
     */
    public ResponseResult login(User user) {
        String username;
        String password;
        if (user != null && StrUtil.isNotEmpty(username = user.getUsername()) && StrUtil.isNotEmpty(password = user.getPassword())) {
            String token = this.authenticate(username, password);
            return token == null ? ResponseResult.error("用户名或密码错误") : ResponseResult.success(new ResponseUser(user, token));
        }
        return ResponseResult.error("登录失败");
    }

    /**
     * 注册
     *
     * @param user 用户信息
     * @return 注册结果
     */
    public ResponseResult register(User user) {
        String username;
        String password;
        if (user != null && StrUtil.isNotEmpty(username = user.getUsername()) && StrUtil.isNotEmpty(password = user.getPassword()) && this.notExists(username)) {
            User register = new User()
                    .setUsername(username)
                    // 加密
                    .setPassword(bCryptPasswordEncoder.encode(password));
            this.save(register);

            return ResponseResult.success(new ResponseUser(user, this.authenticate(username, password)));
        }
        return ResponseResult.error("注册失败");
    }

    /**
     * 退出登录
     *
     * @param request 请求
     */
    public void logout(HttpServletRequest request) {
        // 获取请求头里携带的token
        String token = request.getHeader(header);
        if (StrUtil.isNotEmpty(token) && StrUtil.startWith(token, suffiex)) {
            token = token.substring(suffiex.length());
            // 删除token缓存
            String username = jwtUtil.getUsernameFromToken(token);
            redisTemplate.delete(username);
            // 修改认证信息
            SecurityContextHolder.getContext().getAuthentication().setAuthenticated(false);
        }
    }

    /**
     * 认证
     *
     * @param username 用户名
     * @param password 密码
     * @return token
     */
    private String authenticate(String username, String password) {
        // 认证
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (AuthenticationException e) {
            // 一般只有登录时认证失败才会进入这里
            return null;
        }

        // 将认证信息存储在SecurityContextHolder中
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 获取认证信息，也就是我们实现UserDetails的类
        User principal = (User) authentication.getPrincipal();
        // 生成token
        String token = jwtUtil.generate(principal);
        // 缓存token
        redisTemplate.opsForValue().set(username, token, expired, TimeUnit.SECONDS);

        return token;
    }

    /**
     * 判断是否存在该用户
     *
     * @param username 用户名
     * @return 是否存在
     */
    private boolean notExists(String username) {
        return this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)) < 1;
    }

    /**
     * 根据用户名加载用户信息
     * 当然传入的可不止用户名，只要通过这个参数能获取到用户信息即可，例如手机号，身份证号等
     *
     * @param username 用户名
     * @return 用户信息，用于之后的认证授权
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        Assert.isTrue(user != null, "当前用户不存在");
        return user;
    }
}
```

这时启动如果报错

> Field authenticationManager in com.example.security.service.UserService required a bean of type 'org.springframework.security.authentication.AuthenticationManager' that could not be found.

与之前的报错信息类似的，在配置类中添加

```java
@Bean
@Override
public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
}
```

写到这是不是觉得差不多了，但是又觉得缺了点啥，我们的 JWT 是不是没派上什么用场啊，是的，还缺少一个过滤器，用于判断每次请求中携带的 JWT 是否合法

编写 JwtFilter

```java
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
```

将我们的过滤器添加到配置类中

```java
@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtFilter jwtFilter;

    // TODO 注意
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // TODO 注意
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors()
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf().disable()
                .authorizeRequests()
                // 将test接口移除，用于测试，加入其他无需认证的接口
                .antMatchers("/login", "/register", "/logout")
                .permitAll()
                .anyRequest()
                .authenticated();
        // 添加JWT过滤器
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
```

编写接口

```java
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
```

### 测试

先请求 test 接口，发现报 403，符合我们的预期，调用 login 接口登录，可以获取到一个 token，再次请求 test 接口，并携带上这个 token，就可以正常访问了

## 权限认证

以上的接口都是统一提供给用户的，所有用户能请求的接口都是一样的，但有时我们希望某些用户只能请求某些接口，另一些用户可以请求所有的接口，如果通过硬编码写到代码中，就不提代码的复杂度了，将来需求改变或维护时可就太麻烦了

于是就有了 RBAC 模型，不同的用户有不同的角色，不同的角色有不同的权限，存到数据库或缓存中，在用户请求接口时，判断他的权限做出相应的回应

新增 UserRoleService，用于查询用户的角色

```java
@Service
public class UserRoleService extends ServiceImpl<UserRoleMapper, UserRole> {

    @Autowired
    private RedisTemplate redisTemplate;

    private static final String USER_ROLE = "USER_ROLE";

    /**
     * 获取用户角色，并加入进缓存
     */
    public Map<Long, List<Long>> getUserRole() {
        if (redisTemplate.hasKey(USER_ROLE)) {
            return (Map<Long, List<Long>>) redisTemplate.opsForValue().get(USER_ROLE);
        }

        List<UserRole> list = this.list();

        Map<Long, List<Long>> map = new HashMap<>();

        if (CollUtil.isNotEmpty(list)) {
            for (UserRole userRole : list) {
                List<Long> set = map.computeIfAbsent(userRole.getUserId(), v -> new ArrayList<>());
                set.add(userRole.getRoleId());
            }
        }

        redisTemplate.opsForValue().set(USER_ROLE, map);

        return map;
    }
}
```

新增 RolePermissionService，用于查询角色的权限

```java
@Service
public class RolePermissionService extends ServiceImpl<RolePermissionMapper, RolePermission> {

    @Autowired
    private RedisTemplate redisTemplate;

    private static final String ROLE_PERMISSION = "ROLE_PERMISSION";

    /**
     * 获取角色权限，并加入到缓存
     */
    public Map<Long, Set<String>> getRolePermission() {
        if (redisTemplate.hasKey(ROLE_PERMISSION)) {
            return (Map<Long, Set<String>>) redisTemplate.opsForValue().get(ROLE_PERMISSION);
        }

        List<RolePermission> list = this.getBaseMapper().getAll();

        Map<Long, Set<String>> map = new HashMap<>();

        if (CollUtil.isNotEmpty(list)) {
            for (RolePermission rolePermission : list) {
                Set<String> set = map.computeIfAbsent(rolePermission.getRoleId(), v -> new HashSet<>());
                set.add(rolePermission.getPermission());
            }
        }

        redisTemplate.opsForValue().set(ROLE_PERMISSION, map);

        return map;
    }
}
```

```java
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    @Select("SELECT a.role_id, b.permission FROM role_permission a LEFT JOIN permission b ON a.role_id = b.id;")
    List<RolePermission> getAll();
}
```

在 JwtFilter 中添加权限判断

```java
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

            // 权限判断
            if (!this.check(response, jwtUtil.getUserIdFromToken(token), request.getRequestURI())) {
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
```

### 测试

首先在数据库中添加角色、权限等信息，也可以自己编写接口插入，或者运行 sql 目录下的 `test_security-2.sql`，提供了简短的测试数据

分别获取两个用户的 token，两个用户对应不同的角色，一个角色中有其他角色无法请求的接口，分别携带着这两个 token 去请求这个接口，一个可以正常访问，一个返回没有权限

### 使用注解鉴权

使用注解，相比于使用配置或者统一鉴权，粒度更细，可以针对某个方法进行鉴权，而配置或者统一鉴权都是针对接口进行鉴权

Spring Security 常用 `@PreAuthorize` 进行鉴权，如需使用，先在配置类上加上 `@EnableGlobalMethodSecurity(prePostEnabled = true)` 注解

编写测试方法，对需要鉴权的方法上加上注解就可以，至于注解里面使用 hasAuthority 还是 hasRole 都可以，一般推荐使用 hasAuthority

```java
@Service
public class TestService {

    public void test1() {
        System.out.println("test1");
    }

    @PreAuthorize("hasAuthority('/test')")
    public void test2() {
        System.out.println("test2");
    }

    @PreAuthorize("hasAuthority('/test3')")
    public void test3() {
        System.out.println("test3");
    }
}
```

复用 test 接口

```java
@Autowired
private TestService testService;

@GetMapping("test")
public String test() {
    testService.test1();
    testService.test2();
    testService.test3();
    return "good";
}
```

项目启动后请求该接口，就可在控制台看出只打印了前两个方法，第三个方法没打印出来，而且前端显示 403 错误

## 跨域处理

在配置类中加上

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    // 允许访问的源
    corsConfiguration.setAllowedOrigins(Collections.singletonList("*"));
    // 是否需要携带身份凭证
    corsConfiguration.setAllowCredentials(true);
    // 预检请求的响应结果能够缓存多久
    corsConfiguration.setMaxAge(86400L);
    // 允许携带的请求头
    corsConfiguration.setAllowedHeaders(Collections.singletonList("*"));
    // 允许使用的请求方法
    corsConfiguration.setAllowedMethods(Collections.singletonList("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfiguration);
    return source;
}
```

## 手机验证码登录

手机验证码登录与邮箱验证码登录、扫码登录等流程大差不差，了解了手机验证码登录，再去使用其他登录方式时上手就简单多了

下面只会模拟发验证码操作，不会调用短信 API 真的发送验证码，一方面是我懒，一方面是方便学习调试

```java
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
```

要想实现自己的认证方式，至少要实现 AbstractAuthenticationToken 和 AuthenticationProvider，网上也有说还需实现 AbstractAuthenticationProcessingFilter，我尝试了一下，有些复杂，问题也比较多，所以就略过

自定义一个 AbstractAuthenticationToken，模仿 UsernamePasswordAuthenticationToken 来就行

```java
public class PhoneAuthenticationToken extends AbstractAuthenticationToken {

    /**
     * 手机号
     */
    private Object principal;

    public PhoneAuthenticationToken(Object principal) {
        super(null);
        this.principal = principal;
        super.setAuthenticated(false);
    }

    public PhoneAuthenticationToken(Collection<? extends GrantedAuthority> authorities, Object principal) {
        super(authorities);
        this.principal = principal;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }
}
```

实现 AuthenticationProvider 接口，用于身份校验，

```java
public class PhoneAuthenticationProvider implements AuthenticationProvider {

    private final PhoneLoginService phoneLoginService;

    public PhoneAuthenticationProvider(PhoneLoginService phoneLoginService) {
        this.phoneLoginService = phoneLoginService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        PhoneAuthenticationToken token = (PhoneAuthenticationToken) authentication;
        // 获取手机号
        String phoneNum = (String) token.getPrincipal();
        // 根据手机号获取用户信息
        User user = (User) phoneLoginService.loadUserByUsername(phoneNum);
        token.setAuthenticated(true);
        token.setDetails(user.getId());
        return authentication;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(PhoneAuthenticationToken.class);
    }
}
```

在配置类加上以下配置即可

```java
@Autowired
private PhoneLoginService phoneLoginService;

@Bean
PhoneAuthenticationProvider phoneAuthenticationProvider() {
    return new PhoneAuthenticationProvider(phoneLoginService);
}

@Override
protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    // 如果实现了多个UserDetailsService，需指定哪个Provider用的是哪个UserDetailsService
    auth.authenticationProvider(phoneAuthenticationProvider()).userDetailsService(phoneLoginService);
}
```
