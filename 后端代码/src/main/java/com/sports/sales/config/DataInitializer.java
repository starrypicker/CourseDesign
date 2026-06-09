package com.sports.sales.config;

import com.sports.sales.entity.SysUser;
import com.sports.sales.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initUser("admin", "123456", "admin", null);
        initUser("sun", "123456", "customer", "C001");
        initUser("fly", "123456", "customer", "C002");
        initUser("fit", "123456", "customer", "C003");
        log.info("测试用户初始化完成");
    }

    private void initUser(String username, String rawPassword, String role, String customerCode) {
        if (sysUserMapper.countByUsername(username) > 0) {
            return; // 用户已存在，跳过
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
