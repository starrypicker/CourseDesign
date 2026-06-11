package com.sports.sales.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * HTTPS 配置 - 仅在 server.ssl.enabled=true 时生效
 * 开发环境使用 HTTP 8080 端口，无需此配置
 */
@Configuration
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
public class HttpsConfig {
}
