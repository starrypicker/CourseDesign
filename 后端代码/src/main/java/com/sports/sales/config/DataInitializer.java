package com.sports.sales.config;

import com.sports.sales.entity.SysUser;
import com.sports.sales.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;

    public DataInitializer(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder, DataSource dataSource) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        // 自动创建缺失的表（幂等操作，已存在则跳过）
        ensureTablesExist();

        initUser("admin", "123456", "admin", null);
        initUser("sun", "123456", "customer", "C001");
        initUser("fly", "123456", "customer", "C002");
        initUser("fit", "123456", "customer", "C003");
        log.info("测试用户初始化完成");
    }

    /**
     * 确保发票表等新增表存在，已存在则跳过（幂等）
     */
    private void ensureTablesExist() {
        String createInvoiceTable = """
            CREATE TABLE IF NOT EXISTS invoice (
                id BIGINT AUTO_INCREMENT,
                order_id BIGINT NOT NULL COMMENT '关联订单ID',
                customer_code VARCHAR(20) NOT NULL COMMENT '顾客代码',
                invoice_no VARCHAR(50) COMMENT '发票号码',
                invoice_title VARCHAR(200) NOT NULL COMMENT '发票抬头',
                tax_no VARCHAR(50) COMMENT '税号',
                invoice_type VARCHAR(20) NOT NULL DEFAULT 'personal' COMMENT '发票类型',
                amount DECIMAL(12,2) NOT NULL COMMENT '发票金额',
                invoice_date DATETIME COMMENT '开票日期',
                status TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0-待开票,1-已开票',
                remark VARCHAR(500) COMMENT '备注',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (id),
                KEY idx_invoice_order (order_id),
                KEY idx_invoice_customer (customer_code),
                KEY idx_invoice_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票信息表'
            """;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createInvoiceTable);
            log.info("数据库表结构检查完成");
        } catch (Exception e) {
            log.error("自动建表失败: {}", e.getMessage());
        }
    }

    private void initUser(String username, String rawPassword, String role, String customerCode) {
        if (sysUserMapper.countByUsername(username) > 0) {
            return;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setCustomerCode(customerCode);
        user.setStatus(1);
        sysUserMapper.insert(user);
        log.info("初始化测试用户: username={}, role={}", username, role);
    }
}
