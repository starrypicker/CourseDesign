package com.sports.sales.service;

import com.sports.sales.common.PageResult;
import com.sports.sales.dto.CustomerQueryDTO;
import com.sports.sales.entity.Customer;

public interface CustomerService {

    PageResult<Customer> list(CustomerQueryDTO query);

    Customer getByCode(String customerCode);

    boolean add(Customer customer);

    boolean update(Customer customer);

    boolean delete(String customerCode);
}
