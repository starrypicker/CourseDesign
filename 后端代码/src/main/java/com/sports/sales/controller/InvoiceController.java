package com.sports.sales.controller;

import com.sports.sales.common.PageResult;
import com.sports.sales.common.Result;
import com.sports.sales.entity.Invoice;
import com.sports.sales.service.InvoiceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 发票管理控制器
 * 前台：顾客请求发票
 * 后台：管理员开具发票、查看发票列表
 */
@RestController
@RequestMapping("/api/invoice")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * 发票列表
     */
    @GetMapping("/list")
    public Result<PageResult<Invoice>> list(
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            // 顾客只能看自己的发票
            customerCode = (String) request.getAttribute("customerCode");
        }
        return Result.success(invoiceService.list(customerCode, status, pageNum, pageSize));
    }

    /**
     * 发票详情
     */
    @GetMapping("/{id}")
    public Result<Invoice> getById(@PathVariable Long id) {
        return Result.success(invoiceService.getById(id));
    }

    /**
     * 根据订单ID查询发票
     */
    @GetMapping("/by-order/{orderId}")
    public Result<Invoice> getByOrderId(@PathVariable Long orderId) {
        return Result.success(invoiceService.getByOrderId(orderId));
    }

    /**
     * 顾客请求开票（前台）
     */
    @PostMapping("/request")
    public Result<Long> request(@RequestBody Invoice record, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            record.setCustomerCode((String) request.getAttribute("customerCode"));
        }
        return Result.success(invoiceService.request(record));
    }

    /**
     * 管理员开具发票（后台打印发票）
     */
    @PutMapping("/issue/{id}")
    public Result<Void> issue(@PathVariable Long id,
                               @RequestBody(required = false) Map<String, String> body,
                               HttpServletRequest request) {
        checkAdmin(request);
        String invoiceNo = body != null ? body.get("invoiceNo") : null;
        invoiceService.issue(id, invoiceNo);
        return Result.success();
    }

    /**
     * 取消/作废发票：待开票可取消，已开票可作废
     * 顾客只能操作自己的发票，管理员可操作任意
     */
    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        String customerCode = null;
        if (!"admin".equals(role)) {
            customerCode = (String) request.getAttribute("customerCode");
        }
        invoiceService.cancel(id, customerCode);
        return Result.success();
    }

    private void checkAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            throw new RuntimeException("无权限访问");
        }
    }
}
