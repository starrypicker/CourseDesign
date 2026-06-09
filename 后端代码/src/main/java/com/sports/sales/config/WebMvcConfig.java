package com.sports.sales.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    public WebMvcConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                // 放行登录注册接口
                .excludePathPatterns("/api/auth/**")
                // 放行商品浏览（前台展示不需要登录）
                .excludePathPatterns("/api/product/list", "/api/product/low-stock")
                // 放行厂家查询
                .excludePathPatterns("/api/manufacturer/list")
                // 拦截其他所有 /api/** 请求
                .addPathPatterns("/api/**");
    }
}
