package com.sports.sales.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;

    public WebMvcConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /uploads/** 映射到本地 uploads/ 目录（使用绝对路径避免相对路径问题）
        File uploadDir = new File(uploadBaseDir);
        String absolutePath = uploadDir.getAbsolutePath().replace('\\', '/');
        if (!absolutePath.endsWith("/")) {
            absolutePath += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                // 放行登录注册接口
                .excludePathPatterns("/api/auth/**")
                // 放行商品浏览（前台展示不需要登录）
                .excludePathPatterns("/api/product/list")
                // 放行厂家查询
                .excludePathPatterns("/api/manufacturer/list")
                // 放行静态资源（图片等）
                .excludePathPatterns("/uploads/**")
                // 拦截其他所有 /api/** 请求
                .addPathPatterns("/api/**");
    }
}
