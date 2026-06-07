package com.sports.sales.controller;

import com.sports.sales.common.Result;
import com.sports.sales.entity.Customer;
import com.sports.sales.entity.SysUser;
import com.sports.sales.mapper.CustomerMapper;
import com.sports.sales.mapper.SysUserMapper;
import com.sports.sales.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    private final SysUserMapper sysUserMapper;
    private final CustomerMapper customerMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(SysUserMapper sysUserMapper, CustomerMapper customerMapper,
                          JwtUtil jwtUtil, BCryptPasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.customerMapper = customerMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");
        String role = loginData.get("role");

        if (username == null || password == null) {
            return Result.error(400, "用户名和密码不能为空");
        }

        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            return Result.error(401, "用户名或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }

        // 验证角色匹配
        if (role != null && !role.equals(user.getRole())) {
            return Result.error(403, "角色不匹配，请选择正确的登录入口");
        }

        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(),
                user.getRole(), user.getCustomerCode());

        // 构建用户信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("role", user.getRole());
        userInfo.put("customerCode", user.getCustomerCode());

        // 如果是顾客，关联顾客详细信息
        if ("customer".equals(user.getRole()) && user.getCustomerCode() != null) {
            Customer customer = customerMapper.selectByCode(user.getCustomerCode());
            if (customer != null) {
                userInfo.put("customerName", customer.getCustomerName());
                userInfo.put("contactName", customer.getContactName());
                userInfo.put("address", customer.getAddress());
                userInfo.put("phone", customer.getPhone());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);

        log.info("用户登录成功, username={}, role={}", username, user.getRole());
        return Result.success(result);
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, String> registerData) {
        String username = registerData.get("username");
        String password = registerData.get("password");

        if (username == null || username.trim().isEmpty()) {
            return Result.error(400, "用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            return Result.error(400, "密码长度不能少于6位");
        }

        if (sysUserMapper.countByUsername(username) > 0) {
            return Result.error(400, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("customer");
        user.setStatus(1);

        sysUserMapper.insert(user);
        log.info("用户注册成功, username={}", username);
        return Result.success("注册成功");
    }
}
