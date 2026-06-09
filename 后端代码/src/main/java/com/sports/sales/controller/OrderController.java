package com.sports.sales.controller;

import com.sports.sales.common.PageResult;
import com.sports.sales.common.Result;
import com.sports.sales.dto.OrderCreateDTO;
import com.sports.sales.dto.OrderQueryDTO;
import com.sports.sales.entity.Orders;
import com.sports.sales.service.OrderService;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/list")
    public Result<PageResult<Orders>> list(OrderQueryDTO query) {
        return Result.success(orderService.list(query));
    }

    @GetMapping("/{orderId}")
    public Result<Orders> getById(@PathVariable Long orderId) {
        return Result.success(orderService.getById(orderId));
    }

    @PostMapping
    public Result<Long> createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        log.info("收到创建订单请求, customerCode={}", dto.getCustomerCode());
        return Result.success(orderService.createOrder(dto));
    }

    @PutMapping("/confirm/{orderId}")
    public Result<Void> confirmOrder(@PathVariable Long orderId) {
        log.info("收到确认订单请求, orderId={}", orderId);
        return orderService.confirmOrder(orderId) ? Result.success() : Result.error("确认失败");
    }

    @PutMapping("/ship/{orderId}")
    public Result<Void> shipOrder(@PathVariable Long orderId) {
        log.info("收到发货请求, orderId={}", orderId);
        return orderService.shipOrder(orderId) ? Result.success() : Result.error("发货失败");
    }

    @PutMapping("/cancel/{orderId}")
    public Result<Void> cancelOrder(@PathVariable Long orderId) {
        log.info("收到取消订单请求, orderId={}", orderId);
        return orderService.cancelOrder(orderId) ? Result.success() : Result.error("取消失败");
    }

    @PutMapping("/pay/{orderId}")
    public Result<Void> payOrder(@PathVariable Long orderId, @RequestParam String paymentMethod) {
        log.info("收到付款请求, orderId={}, paymentMethod={}", orderId, paymentMethod);
        return orderService.payOrder(orderId, paymentMethod) ? Result.success() : Result.error("付款失败");
    }

    @PutMapping("/complete/{orderId}")
    public Result<Void> completeOrder(@PathVariable Long orderId) {
        log.info("收到完成订单请求, orderId={}", orderId);
        return orderService.completeOrder(orderId) ? Result.success() : Result.error("完成失败");
    }

    @GetMapping("/unpaid")
    public Result<List<Orders>> getUnpaidOrders() {
        return Result.success(orderService.getUnpaidOrders());
    }

    @GetMapping("/unshipped")
    public Result<List<Orders>> getUnshippedOrders() {
        return Result.success(orderService.getUnshippedOrders());
    }

    @GetMapping("/completed")
    public Result<List<Orders>> getCompletedOrders() {
        return Result.success(orderService.getCompletedOrders());
    }

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/dashboard-stats")
    public Result<java.util.Map<String, Object>> getDashboardStats() {
        return Result.success(orderService.getDashboardStats());
    }
}
