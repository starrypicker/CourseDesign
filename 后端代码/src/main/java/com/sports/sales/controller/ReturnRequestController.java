package com.sports.sales.controller;

import com.sports.sales.common.PageResult;
import com.sports.sales.common.Result;
import com.sports.sales.entity.ReturnRequest;
import com.sports.sales.service.ReturnRequestService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/return")
public class ReturnRequestController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReturnRequestController.class);

    private final ReturnRequestService returnRequestService;

    public ReturnRequestController(ReturnRequestService returnRequestService) {
        this.returnRequestService = returnRequestService;
    }

    /** 获取退换货列表 — 管理员看全部，顾客只看自己的 */
    @GetMapping("/list")
    public Result<PageResult<ReturnRequest>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        String customerCode = null;
        if (!"admin".equals(role)) {
            customerCode = (String) request.getAttribute("customerCode");
        }
        return Result.success(returnRequestService.list(customerCode, status, type, pageNum, pageSize));
    }

    /** 获取退换货详情 */
    @GetMapping("/{id}")
    public Result<ReturnRequest> getById(@PathVariable Long id, HttpServletRequest request) {
        ReturnRequest record = returnRequestService.getById(id);
        checkAccess(request, record);
        return Result.success(record);
    }

    /** 顾客提交退换货申请 */
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String customerCode = (String) request.getAttribute("customerCode");
        if (customerCode == null) {
            return Result.error(400, "无法识别用户身份");
        }
        Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
        Integer type = body.get("type") != null ? Integer.valueOf(body.get("type").toString()) : 0;
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;

        if (orderId == null) {
            return Result.error(400, "订单ID不能为空");
        }
        ReturnRequest record = new ReturnRequest();
        record.setOrderId(orderId);
        record.setCustomerCode(customerCode);
        record.setType(type);
        record.setReason(reason);
        log.info("收到退换货申请, orderId={}, customerCode={}, type={}", orderId, customerCode, type);
        return Result.success(returnRequestService.create(record));
    }

    /** 管理员通过申请 */
    @PutMapping("/approve/{id}")
    public Result<Void> approve(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                 HttpServletRequest request) {
        checkAdmin(request);
        String adminRemark = body.get("adminRemark") != null ? body.get("adminRemark").toString() : null;
        BigDecimal refundAmount = body.get("refundAmount") != null ? new BigDecimal(body.get("refundAmount").toString()) : null;
        String handledBy = (String) request.getAttribute("username");
        log.info("退换货申请通过, id={}", id);
        return returnRequestService.approve(id, adminRemark, refundAmount, handledBy) ? Result.success() : Result.error("操作失败");
    }

    /** 管理员拒绝申请 */
    @PutMapping("/reject/{id}")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        checkAdmin(request);
        String adminRemark = body.get("adminRemark") != null ? body.get("adminRemark").toString() : "管理员拒绝";
        String handledBy = (String) request.getAttribute("username");
        log.info("退换货申请拒绝, id={}", id);
        return returnRequestService.reject(id, adminRemark, handledBy) ? Result.success() : Result.error("操作失败");
    }

    /** 管理员完成退换货（退货退款/换货处理） */
    @PutMapping("/complete/{id}")
    public Result<Void> complete(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        log.info("退换货完成处理, id={}", id);
        return returnRequestService.complete(id) ? Result.success() : Result.error("操作失败");
    }

    private void checkAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            throw new RuntimeException("无权限访问");
        }
    }

    private void checkAccess(HttpServletRequest request, ReturnRequest record) {
        if (record == null) {
            throw new RuntimeException("退换货申请不存在");
        }
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            String customerCode = (String) request.getAttribute("customerCode");
            if (customerCode == null || !customerCode.equals(record.getCustomerCode())) {
                throw new RuntimeException("无权访问该申请");
            }
        }
    }
}
