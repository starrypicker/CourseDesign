package com.sports.sales.mapper;

import com.sports.sales.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserMapper {

    SysUser selectByUsername(@Param("username") String username);

    int insert(SysUser user);

    int countByUsername(@Param("username") String username);

    /** 根据 customerCode 更新系统用户密码 */
    int updatePasswordByCustomerCode(@Param("customerCode") String customerCode, @Param("password") String password);
}
