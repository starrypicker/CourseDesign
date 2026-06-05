package com.sports.sales.service.impl;

import com.sports.sales.common.PageResult;
import com.sports.sales.dto.CustomerQueryDTO;
import com.sports.sales.entity.Customer;
import com.sports.sales.mapper.CustomerMapper;
import com.sports.sales.service.CustomerService;
import com.sports.sales.util.CryptoUtil;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerMapper customerMapper;
    private final CryptoUtil cryptoUtil;

    public CustomerServiceImpl(CustomerMapper customerMapper, CryptoUtil cryptoUtil) {
        this.customerMapper = customerMapper;
        this.cryptoUtil = cryptoUtil;
    }

    @Override
    public PageResult<Customer> list(CustomerQueryDTO query) {
        if (query.getPhone() != null && !query.getPhone().isEmpty()) {
            query.setPhone(cryptoUtil.encrypt(query.getPhone()));
        }
        Long total = customerMapper.selectCount(query);
        List<Customer> rows = customerMapper.selectList(query);
        rows.forEach(c -> {
            decryptSensitiveData(c);
            c.setPassword(null);
        });
        return new PageResult<>(total, rows, query.getPageNum(), query.getPageSize());
    }

    @Override
    @Cacheable(value = "customer", key = "#customerCode")
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
        log.info("添加顾客, customerCode={}, customerName={}", customer.getCustomerCode(), customer.getCustomerName());
        encryptSensitiveData(customer);
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
        return customerMapper.updateByCode(customer) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "customer", key = "#customerCode")
    public boolean delete(String customerCode) {
        log.info("删除顾客, customerCode={}", customerCode);
        return customerMapper.deleteByCode(customerCode) > 0;
    }

    private void encryptSensitiveData(Customer customer) {
        if (customer.getPhone() != null) {
            customer.setPhone(cryptoUtil.encrypt(customer.getPhone()));
        }
        if (customer.getEmail() != null) {
            customer.setEmail(cryptoUtil.encrypt(customer.getEmail()));
        }
    }

    private void decryptSensitiveData(Customer customer) {
        if (customer.getPhone() != null) {
            customer.setPhone(tryDecrypt(customer.getPhone()));
        }
        if (customer.getEmail() != null) {
            customer.setEmail(tryDecrypt(customer.getEmail()));
        }
    }

    /**
     * 尝试解密，如果数据是明文（未加密），则直接返回原文
     */
    private String tryDecrypt(String value) {
        try {
            return cryptoUtil.decrypt(value);
        } catch (Exception e) {
            // 解密失败说明是明文数据，直接返回
            return value;
        }
    }

    @Override
    public Customer login(String customerCode, String password) {
        log.info("顾客登录, customerCode={}", customerCode);
        Customer customer = customerMapper.selectByCodeAndPassword(customerCode, password);
        if (customer != null) {
            decryptSensitiveData(customer);
            // 不返回密码
            customer.setPassword(null);
        }
        return customer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "customer", key = "#customerCode")
    public boolean changePassword(String customerCode, String oldPassword, String newPassword) {
        log.info("修改密码, customerCode={}", customerCode);
        Customer customer = customerMapper.selectByCodeAndPassword(customerCode, oldPassword);
        if (customer == null) {
            return false;
        }
        customer.setPassword(newPassword);
        return customerMapper.updateByCode(customer) > 0;
    }
}
