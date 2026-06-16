package com.sports.sales.controller;

import com.sports.sales.common.PageResult;
import com.sports.sales.common.Result;
import com.sports.sales.dto.ProductQueryDTO;
import com.sports.sales.entity.Product;
import com.sports.sales.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;

    /** 获取上传目录的绝对路径 */
    private Path getUploadPath() throws IOException {
        File dir = new File(uploadBaseDir, "products");
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new IOException("无法创建上传目录: " + dir.getAbsolutePath());
            }
        }
        log.info("上传目录: {}", dir.getAbsolutePath());
        return dir.toPath();
    }

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 上传单个商品图片
     */
    @PostMapping("/{productCode}/upload-image")
    public Result<Map<String, String>> uploadImage(@PathVariable String productCode,
                                                    @RequestParam("file") MultipartFile file,
                                                    HttpServletRequest request) {
        checkAdmin(request);
        try {
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只允许上传图片文件");
            }
            // 限制文件大小 (5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.error("图片大小不能超过5MB");
            }

            // 确保上传目录存在
            Path uploadPath = getUploadPath();

            // 生成文件名：商品编码 + 原始扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = productCode + extension;

            // 保存文件
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            // 更新商品的 image 字段（如果商品已存在）
            String imageUrl = "/uploads/products/" + filename;
            Product product = productService.getByCode(productCode);
            if (product != null) {
                product.setImage(imageUrl);
                productService.update(product);
                log.info("商品图片已关联, productCode={}", productCode);
            } else {
                log.info("商品尚未创建，图片已保存，productCode={}", productCode);
            }

            Map<String, String> result = new HashMap<>();
            result.put("url", imageUrl);
            result.put("filename", filename);
            log.info("商品图片上传成功, productCode={}, url={}", productCode, imageUrl);
            return Result.success(result);
        } catch (IOException e) {
            log.error("图片上传失败, productCode={}", productCode, e);
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量导入商品图片（通过ZIP包或匹配文件名）
     * 文件名格式：商品编码.jpg/png → 自动匹配商品
     */
    @PostMapping("/batch-upload-images")
    public Result<Map<String, Object>> batchUploadImages(@RequestParam("files") List<MultipartFile> files,
                                                          HttpServletRequest request) {
        checkAdmin(request);
        int success = 0;
        int fail = 0;
        List<String> errors = new ArrayList<>();

        try {
            Path uploadPath = getUploadPath();

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                try {
                    String contentType = file.getContentType();
                    if (contentType == null || !contentType.startsWith("image/")) {
                        fail++;
                        errors.add(file.getOriginalFilename() + " — 不是图片文件");
                        continue;
                    }

                    String originalFilename = file.getOriginalFilename();
                    if (originalFilename == null) { fail++; continue; }

                    // 从文件名提取商品编码（去掉扩展名）
                    String productCode = originalFilename;
                    if (productCode.contains(".")) {
                        productCode = productCode.substring(0, productCode.lastIndexOf("."));
                    }

                    // 验证商品是否存在
                    Product product = productService.getByCode(productCode);
                    if (product == null) {
                        fail++;
                        errors.add(originalFilename + " — 未找到对应商品: " + productCode);
                        continue;
                    }

                    // 保存文件
                    String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    String filename = productCode + extension;
                    Path filePath = uploadPath.resolve(filename);
                    log.info("正在保存图片: {} -> {}", originalFilename, filePath.toAbsolutePath());
                    file.transferTo(filePath.toFile());

                    // 更新商品 image 字段
                    String imageUrl = "/uploads/products/" + filename;
                    product.setImage(imageUrl);
                    productService.update(product);
                    success++;
                    log.info("图片导入成功: {} -> {}", originalFilename, imageUrl);
                } catch (Exception e) {
                    fail++;
                    String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    log.error("图片导入失败: {} — {}", file.getOriginalFilename(), msg, e);
                    errors.add(file.getOriginalFilename() + " — " + msg);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("fail", fail);
            result.put("errors", errors);
            log.info("批量导入图片完成, 成功={}, 失败={}", success, fail);
            return Result.success(result);
        } catch (IOException e) {
            log.error("批量导入图片失败", e);
            return Result.error("批量导入失败: " + e.getMessage());
        }
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
    public Result<Void> add(@Valid @RequestBody Product product, HttpServletRequest request) {
        checkAdmin(request);
        log.info("收到添加商品请求, productCode={}", product.getProductCode());
        return productService.add(product) ? Result.success() : Result.error("添加失败");
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody Product product, HttpServletRequest request) {
        checkAdmin(request);
        log.info("收到更新商品请求, productCode={}", product.getProductCode());
        return productService.update(product) ? Result.success() : Result.error("更新失败");
    }

    @DeleteMapping("/{productCode}")
    public Result<Void> delete(@PathVariable String productCode, HttpServletRequest request) {
        checkAdmin(request);
        log.info("收到删除商品请求, productCode={}", productCode);
        return productService.delete(productCode) ? Result.success() : Result.error("删除失败");
    }

    @GetMapping("/low-stock")
    public Result<List<Product>> getLowStockProducts() {
        return Result.success(productService.getLowStockProducts());
    }

    @PostMapping("/replenish")
    public Result<Void> replenishStock(@RequestParam String productCode, @RequestParam Integer quantity,
                                        HttpServletRequest request) {
        checkAdmin(request);
        log.info("收到补货请求, productCode={}, quantity={}", productCode, quantity);
        return productService.replenishStock(productCode, quantity) ? Result.success() : Result.error("补货失败");
    }

    private void checkAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            throw new RuntimeException("无权限访问");
        }
    }
}
