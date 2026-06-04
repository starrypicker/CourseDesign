package com.sports.sales.controller;

import com.sports.sales.common.PageResult;
import com.sports.sales.common.Result;
import com.sports.sales.dto.CustomerQueryDTO;
import com.sports.sales.entity.Customer;
import com.sports.sales.service.CustomerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/list")
    public Result<PageResult<Customer>> list(CustomerQueryDTO query) {
        return Result.success(customerService.list(query));
    }

    @GetMapping("/{customerCode}")
    public Result<Customer> getByCode(@PathVariable String customerCode) {
        return Result.success(customerService.getByCode(customerCode));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Customer customer) {
        log.info("收到添加顾客请求, customerCode={}", customer.getCustomerCode());
        return customerService.add(customer) ? Result.success() : Result.error("添加失败");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Customer customer) {
        log.info("收到更新顾客请求, customerCode={}", customer.getCustomerCode());
        return customerService.update(customer) ? Result.success() : Result.error("更新失败");
    }

    @DeleteMapping("/{customerCode}")
    public Result<Void> delete(@PathVariable String customerCode) {
        log.info("收到删除顾客请求, customerCode={}", customerCode);
        return customerService.delete(customerCode) ? Result.success() : Result.error("删除失败");
    }
}
