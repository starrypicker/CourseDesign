package com.sports.sales.controller;

import com.sports.sales.common.PageResult;
import com.sports.sales.common.Result;
import com.sports.sales.dto.CustomerQueryDTO;
import com.sports.sales.entity.Customer;
import com.sports.sales.service.CustomerService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * 获取顾客列表 — 仅管理员
     */
    @GetMapping("/list")
    public Result<PageResult<Customer>> list(CustomerQueryDTO query) {
        return Result.success(customerService.list(query));
    }

    /**
     * 获取顾客详情 — 顾客只能看自己，管理员可看任意
     */
    @GetMapping("/{customerCode}")
    public Result<Customer> getByCode(@PathVariable String customerCode, HttpServletRequest request) {
        checkCustomerAccess(request, customerCode);
        return Result.success(customerService.getByCode(customerCode));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Customer customer) {
        log.info("收到添加顾客请求, customerCode={}", customer.getCustomerCode());
        return customerService.add(customer) ? Result.success() : Result.error("添加失败");
    }

    /**
     * 更新顾客 — 顾客只能更新自己，管理员可更新任意
     */
    @PutMapping
    public Result<Void> update(@RequestBody Customer customer, HttpServletRequest request) {
        checkCustomerAccess(request, customer.getCustomerCode());
        log.info("收到更新顾客请求, customerCode={}", customer.getCustomerCode());
        return customerService.update(customer) ? Result.success() : Result.error("更新失败");
    }

    @DeleteMapping("/{customerCode}")
    public Result<Void> delete(@PathVariable String customerCode) {
        log.info("收到删除顾客请求, customerCode={}", customerCode);
        return customerService.delete(customerCode) ? Result.success() : Result.error("删除失败");
    }

    @PutMapping("/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> passwordData,
                                       HttpServletRequest request) {
        // 从JWT Token中获取当前用户的customerCode，防止越权
        String customerCode = (String) request.getAttribute("customerCode");
        if (customerCode == null) {
            // 如果token中没有customerCode（如管理员），则使用请求体中的值
            customerCode = passwordData.get("customerCode");
        }
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        if (customerCode == null || oldPassword == null || newPassword == null) {
            return Result.error(400, "参数不完整");
        }
        log.info("收到修改密码请求, customerCode={}", customerCode);
        return customerService.changePassword(customerCode, oldPassword, newPassword) ? Result.success() : Result.error("原密码错误");
    }

    /**
     * 校验顾客访问权限：管理员可操作所有，顾客只能操作自己
     */
    private void checkCustomerAccess(HttpServletRequest request, String targetCustomerCode) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            String tokenCustomerCode = (String) request.getAttribute("customerCode");
            if (tokenCustomerCode == null || !tokenCustomerCode.equals(targetCustomerCode)) {
                throw new RuntimeException("无权访问该顾客信息");
            }
        }
    }
}
