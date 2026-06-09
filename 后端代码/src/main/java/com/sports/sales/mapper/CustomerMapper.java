package com.sports.sales.mapper;

import com.sports.sales.entity.Customer;
import com.sports.sales.dto.CustomerQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CustomerMapper {

    List<Customer> selectList(CustomerQueryDTO query);

    Long selectCount(CustomerQueryDTO query);

    Customer selectByCode(@Param("customerCode") String customerCode);

    int insert(Customer customer);

    int updateByCode(Customer customer);

    int deleteByCode(@Param("customerCode") String customerCode);

    int updatePasswordByCode(@Param("customerCode") String customerCode, @Param("password") String password);
}
