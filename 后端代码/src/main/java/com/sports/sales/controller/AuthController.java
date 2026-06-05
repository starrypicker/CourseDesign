package com.sports.sales.controller;

import com.sports.sales.common.Result;
import com.sports.sales.entity.Customer;
import com.sports.sales.service.CustomerService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    // 管理员账号（硬编码，生产环境应使用数据库）
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private final CustomerService customerService;

    public AuthController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");
        String role = loginData.get("role");

        log.info("登录请求, username={}, role={}", username, role);

        if (username == null || password == null) {
            return Result.error("用户名和密码不能为空");
        }

        Map<String, Object> data = new HashMap<>();

        if ("admin".equals(role)) {
            // 管理员登录
            if (ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password)) {
                String token = UUID.randomUUID().toString().replace("-", "");
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("customerCode", "admin");
                userInfo.put("customerName", "管理员");
                userInfo.put("role", "admin");
                data.put("token", token);
                data.put("userInfo", userInfo);
                return Result.success(data);
            } else {
                return Result.error("管理员账号或密码错误");
            }
        } else {
            // 顾客登录
            Customer customer = customerService.login(username, password);
            if (customer != null) {
                if (customer.getStatus() != null && customer.getStatus() == 0) {
                    return Result.error("账号已被禁用");
                }
                String token = UUID.randomUUID().toString().replace("-", "");
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("customerCode", customer.getCustomerCode());
                userInfo.put("customerName", customer.getCustomerName());
                userInfo.put("contactName", customer.getContactName());
                userInfo.put("email", customer.getEmail());
                userInfo.put("phone", customer.getPhone());
                userInfo.put("address", customer.getAddress());
                userInfo.put("role", "customer");
                data.put("token", token);
                data.put("userInfo", userInfo);
                return Result.success(data);
            } else {
                return Result.error("用户名或密码错误");
            }
        }
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, String> registerData) {
        String customerCode = registerData.get("username");
        String password = registerData.get("password");
        String customerName = registerData.get("customerName");

        log.info("注册请求, customerCode={}", customerCode);

        if (customerCode == null || password == null) {
            return Result.error("用户名和密码不能为空");
        }

        // 检查用户名是否已存在
        Customer existing = customerService.getByCode(customerCode);
        if (existing != null) {
            return Result.error("用户名已存在");
        }

        Customer customer = new Customer();
        customer.setCustomerCode(customerCode);
        customer.setCustomerName(customerName != null ? customerName : customerCode);
        customer.setPassword(password);
        customer.setStatus(1);

        boolean result = customerService.add(customer);
        return result ? Result.success() : Result.error("注册失败");
    }
}
