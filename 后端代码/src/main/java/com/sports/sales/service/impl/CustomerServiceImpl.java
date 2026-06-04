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
        query.setPageNum((query.getPageNum() - 1) * query.getPageSize());
        Long total = customerMapper.selectCount(query);
        List<Customer> rows = customerMapper.selectList(query);
        rows.forEach(this::decryptSensitiveData);
        return new PageResult<>(total, rows, query.getPageNum() / query.getPageSize() + 1, query.getPageSize());
    }

    @Override
    @Cacheable(value = "customer", key = "#customerCode")
    public Customer getByCode(String customerCode) {
        Customer customer = customerMapper.selectByCode(customerCode);
        if (customer != null) {
            decryptSensitiveData(customer);
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
            customer.setPhone(cryptoUtil.decrypt(customer.getPhone()));
        }
        if (customer.getEmail() != null) {
            customer.setEmail(cryptoUtil.decrypt(customer.getEmail()));
        }
    }
}
