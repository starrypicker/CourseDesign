package com.sports.sales.controller;

import com.sports.sales.common.Result;
import com.sports.sales.entity.Manufacturer;
import com.sports.sales.service.ManufacturerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manufacturer")
public class ManufacturerController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ManufacturerController.class);

    private final ManufacturerService manufacturerService;

    public ManufacturerController(ManufacturerService manufacturerService) {
        this.manufacturerService = manufacturerService;
    }

    @GetMapping("/list")
    public Result<List<Manufacturer>> list() {
        return Result.success(manufacturerService.list());
    }

    @GetMapping("/{manufacturerCode}")
    public Result<Manufacturer> getByCode(@PathVariable String manufacturerCode) {
        return Result.success(manufacturerService.getByCode(manufacturerCode));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Manufacturer manufacturer, HttpServletRequest request) {
        checkAdmin(request);
        log.info("收到添加厂家请求, manufacturerCode={}", manufacturer.getManufacturerCode());
        return manufacturerService.add(manufacturer) ? Result.success() : Result.error("添加失败");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Manufacturer manufacturer, HttpServletRequest request) {
        checkAdmin(request);
        log.info("收到更新厂家请求, manufacturerCode={}", manufacturer.getManufacturerCode());
        return manufacturerService.update(manufacturer) ? Result.success() : Result.error("更新失败");
    }

    @DeleteMapping("/{manufacturerCode}")
    public Result<Void> delete(@PathVariable String manufacturerCode, HttpServletRequest request) {
        checkAdmin(request);
        log.info("收到删除厂家请求, manufacturerCode={}", manufacturerCode);
        return manufacturerService.delete(manufacturerCode) ? Result.success() : Result.error("删除失败");
    }

    private void checkAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            throw new RuntimeException("无权限访问");
        }
    }
}
