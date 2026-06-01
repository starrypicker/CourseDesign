package com.sports.sales.controller;

import com.sports.sales.common.PageResult;
import com.sports.sales.common.Result;
import com.sports.sales.dto.ProductQueryDTO;
import com.sports.sales.entity.Product;
import com.sports.sales.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/list")
    public Result<PageResult<Product>> list(ProductQueryDTO query) {
        return Result.success(productService.list(query));
    }

    @GetMapping("/{productCode}")
    public Result<Product> getByCode(@PathVariable String productCode) {
        return Result.success(productService.getByCode(productCode));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Product product) {
        return productService.add(product) ? Result.success() : Result.error("添加失败");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Product product) {
        return productService.update(product) ? Result.success() : Result.error("更新失败");
    }

    @DeleteMapping("/{productCode}")
    public Result<Void> delete(@PathVariable String productCode) {
        return productService.delete(productCode) ? Result.success() : Result.error("删除失败");
    }

    @GetMapping("/low-stock")
    public Result<List<Product>> getLowStockProducts() {
        return Result.success(productService.getLowStockProducts());
    }

    @PostMapping("/replenish")
    public Result<Void> replenishStock(@RequestParam String productCode, @RequestParam Integer quantity) {
        return productService.replenishStock(productCode, quantity) ? Result.success() : Result.error("补货失败");
    }
}
