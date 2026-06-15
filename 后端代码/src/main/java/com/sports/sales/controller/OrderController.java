package com.sports.sales.controller;

import com.sports.sales.common.PageResult;
import com.sports.sales.common.Result;
import com.sports.sales.dto.OrderCreateDTO;
import com.sports.sales.dto.OrderQueryDTO;
import com.sports.sales.entity.Orders;
import com.sports.sales.service.OrderService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
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

    /**
     * 获取订单列表 — 顾客只能看自己的订单，管理员看全部
     */
    @GetMapping("/list")
    public Result<PageResult<Orders>> list(OrderQueryDTO query, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            String customerCode = (String) request.getAttribute("customerCode");
            if (customerCode != null) {
                query.setCustomerCode(customerCode);
            }
        }
        return Result.success(orderService.list(query));
    }

    /**
     * 获取订单详情 — 顾客只能看自己的订单
     */
    @GetMapping("/{orderId}")
    public Result<Orders> getById(@PathVariable Long orderId, HttpServletRequest request) {
        Orders order = orderService.getById(orderId);
        checkOrderAccess(request, order);
        return Result.success(order);
    }

    @PostMapping
    public Result<Long> createOrder(@Valid @RequestBody OrderCreateDTO dto, HttpServletRequest request) {
        // 顾客只能为自己创建订单，管理员可为任意顾客创建
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            String tokenCustomerCode = (String) request.getAttribute("customerCode");
            if (tokenCustomerCode != null && !tokenCustomerCode.equals(dto.getCustomerCode())) {
                return Result.error(403, "只能为自己创建订单");
            }
        }
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
    public Result<Void> cancelOrder(@PathVariable Long orderId, HttpServletRequest request) {
        Orders order = orderService.getById(orderId);
        checkOrderAccess(request, order);
        log.info("收到取消订单请求, orderId={}", orderId);
        return orderService.cancelOrder(orderId) ? Result.success() : Result.error("取消失败");
    }

    @PutMapping("/pay/{orderId}")
    public Result<Void> payOrder(@PathVariable Long orderId, @RequestParam String paymentMethod,
                                  HttpServletRequest request) {
        Orders order = orderService.getById(orderId);
        checkOrderAccess(request, order);
        log.info("收到付款请求, orderId={}, paymentMethod={}", orderId, paymentMethod);
        return orderService.payOrder(orderId, paymentMethod) ? Result.success() : Result.error("付款失败");
    }

    @PutMapping("/complete/{orderId}")
    public Result<Void> completeOrder(@PathVariable Long orderId, HttpServletRequest request) {
        Orders order = orderService.getById(orderId);
        checkOrderAccess(request, order);
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

    @GetMapping("/out-of-stock")
    public Result<List<Orders>> getOutOfStockOrders(HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(orderService.getOutOfStockOrders());
    }

    @PutMapping("/mark-suppliable/{orderId}")
    public Result<Void> markAsSuppliable(@PathVariable Long orderId, HttpServletRequest request) {
        checkAdmin(request);
        log.info("收到标记可供货请求, orderId={}", orderId);
        return orderService.markAsSuppliable(orderId) ? Result.success() : Result.error("标记失败");
    }

    /**
     * 获取仪表盘统计数据（仅管理员调用）
     */
    @GetMapping("/dashboard-stats")
    public Result<java.util.Map<String, Object>> getDashboardStats() {
        return Result.success(orderService.getDashboardStats());
    }

    /**
     * 获取订单邮寄信息（用于打印邮寄标签，管理员调用）
     */
    @GetMapping("/shipping-info/{orderId}")
    public Result<java.util.Map<String, Object>> getShippingInfo(@PathVariable Long orderId, HttpServletRequest request) {
        checkAdmin(request);
        Orders order = orderService.getById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        java.util.Map<String, Object> info = new java.util.LinkedHashMap<>();
        info.put("orderId", order.getOrderId());
        info.put("recipientName", order.getRecipientName());
        info.put("recipientPhone", order.getRecipientPhone());
        info.put("recipientAddress", order.getRecipientAddress());
        info.put("shippingRequirement", order.getShippingRequirement());
        info.put("totalWeight", order.getTotalWeight());
        info.put("shippingFee", order.getShippingFee());
        info.put("shippingDate", order.getShippingDate());
        info.put("shippingStatus", order.getShippingStatus());
        return Result.success(info);
    }

    private void checkAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            throw new RuntimeException("无权限访问");
        }
    }

    /**
     * 校验订单访问权限：管理员可查看所有，顾客只能查看自己的订单
     */
    private void checkOrderAccess(HttpServletRequest request, Orders order) {
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            String customerCode = (String) request.getAttribute("customerCode");
            if (customerCode == null || !customerCode.equals(order.getCustomerCode())) {
                throw new RuntimeException("无权访问该订单");
            }
        }
    }
}
