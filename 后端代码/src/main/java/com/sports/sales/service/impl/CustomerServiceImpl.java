package com.sports.sales.service.impl;

import com.sports.sales.common.PageResult;
import com.sports.sales.dto.CustomerQueryDTO;
import com.sports.sales.entity.Customer;
import com.sports.sales.mapper.CustomerMapper;
import com.sports.sales.mapper.SysUserMapper;
import com.sports.sales.service.CustomerService;
import com.sports.sales.util.CryptoUtil;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerMapper customerMapper;
    private final CryptoUtil cryptoUtil;
    private final PasswordEncoder passwordEncoder;
    private final SysUserMapper sysUserMapper;

    public CustomerServiceImpl(CustomerMapper customerMapper, CryptoUtil cryptoUtil,
                               PasswordEncoder passwordEncoder, SysUserMapper sysUserMapper) {
        this.customerMapper = customerMapper;
        this.cryptoUtil = cryptoUtil;
        this.passwordEncoder = passwordEncoder;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public PageResult<Customer> list(CustomerQueryDTO query) {
        Long total = customerMapper.selectCount(query);
        List<Customer> rows = customerMapper.selectList(query);
        rows.forEach(c -> {
            decryptSensitiveData(c);
            c.setPassword(null);
        });
        return new PageResult<>(total, rows, query.getPageNum(), query.getPageSize());
    }

    @Override
    @Cacheable(value = "customer", key = "#customerCode", unless = "#result == null")
    public Customer getByCode(String customerCode) {
        Customer customer = customerMapper.selectByCode(customerCode);
        if (customer != null) {
            decryptSensitiveData(customer);
            customer.setPassword(null);
        }
        return customer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean add(Customer customer) {
        log.info("添加顾客, customerCode={}, customerName={}",
                customer.getCustomerCode(), customer.getCustomerName());
        // 检查顾客编码是否已存在
        if (customerMapper.selectByCode(customer.getCustomerCode()) != null) {
            throw new RuntimeException("顾客编码 " + customer.getCustomerCode() + " 已存在");
        }
        encryptSensitiveData(customer);
        // BCrypt 哈希密码，如果未提供则使用默认密码
        if (customer.getPassword() != null && !customer.getPassword().isEmpty()) {
            customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        } else {
            customer.setPassword(passwordEncoder.encode("123456"));
        }
        customer.setStatus(1);
        boolean result = customerMapper.insert(customer) > 0;
        if (result) {
            log.info("顾客添加成功, customerCode={}", customer.getCustomerCode());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "customer", key = "#customer.customerCode")
    public boolean update(Customer customer) {
        log.info("更新顾客, customerCode={}", customer.getCustomerCode());
        encryptSensitiveData(customer);
        // 如果传入了新密码，则哈希后存储；否则不修改密码
        if (customer.getPassword() != null && !customer.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(customer.getPassword());
            customer.setPassword(encodedPassword);
            // 同步更新 sys_user 表的密码，确保登录验证一致
            sysUserMapper.updatePasswordByCustomerCode(customer.getCustomerCode(), encodedPassword);
        } else {
            customer.setPassword(null); // 不修改密码字段
        }
        return customerMapper.updateByCode(customer) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "customer", key = "#customerCode")
    public boolean delete(String customerCode) {
        log.info("删除顾客, customerCode={}", customerCode);
        return customerMapper.deleteByCode(customerCode) > 0;
    }

    @Override
    public Customer login(String customerCode, String password) {
        log.info("顾客登录, customerCode={}", customerCode);
        Customer customer = customerMapper.selectByCode(customerCode);
        if (customer == null) {
            return null;
        }

        // BCrypt 密码验证，兼容旧明文密码
        boolean passwordMatch = verifyPassword(password, customer.getPassword());
        if (!passwordMatch) {
            log.warn("密码验证失败, customerCode={}", customerCode);
            return null;
        }

        // 如果是旧明文密码，自动升级为BCrypt哈希
        if (needPasswordUpgrade(customer.getPassword())) {
            customer.setPassword(passwordEncoder.encode(password));
            customerMapper.updateByCode(customer);
            log.info("密码已自动升级为BCrypt哈希, customerCode={}", customerCode);
        }

        decryptSensitiveData(customer);
        customer.setPassword(null);
        return customer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "customer", key = "#customerCode")
    public boolean changePassword(String customerCode, String oldPassword, String newPassword) {
        log.info("修改密码, customerCode={}", customerCode);
        Customer customer = customerMapper.selectByCode(customerCode);
        if (customer == null) {
            return false;
        }

        // BCrypt 验证旧密码
        if (!verifyPassword(oldPassword, customer.getPassword())) {
            return false;
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        // 同步更新 sys_user 表密码
        sysUserMapper.updatePasswordByCustomerCode(customerCode, encodedPassword);
        return customerMapper.updatePasswordByCode(customerCode, encodedPassword) > 0;
    }

    /**
     * 验证密码（BCrypt + 明文兼容）
     */
    private boolean verifyPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        // BCrypt 哈希以 $2a$、$2b$、$2y$ 开头
        if (storedPassword.startsWith("$2")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        // 兼容旧明文密码
        return storedPassword.equals(rawPassword);
    }

    /**
     * 判断密码是否需要升级（旧明文密码）
     */
    private boolean needPasswordUpgrade(String storedPassword) {
        return storedPassword != null && !storedPassword.startsWith("$2");
    }

    private void encryptSensitiveData(Customer customer) {
        if (customer.getPhone() != null && !customer.getPhone().isEmpty()) {
            customer.setPhone(cryptoUtil.encrypt(customer.getPhone()));
        }
        if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
            customer.setEmail(cryptoUtil.encrypt(customer.getEmail()));
        }
    }

    private void decryptSensitiveData(Customer customer) {
        if (customer.getPhone() != null && !customer.getPhone().isEmpty()) {
            customer.setPhone(tryDecrypt(customer.getPhone()));
        }
        if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
            customer.setEmail(tryDecrypt(customer.getEmail()));
        }
    }

    /**
     * 尝试解密，如果数据是明文（未加密或旧格式），则直接返回原文
     */
    private String tryDecrypt(String value) {
        try {
            return cryptoUtil.decrypt(value);
        } catch (Exception e) {
            // 解密失败说明是明文数据或旧格式，直接返回
            return value;
        }
    }
}
