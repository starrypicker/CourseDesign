package com.sports.sales.mapper;

import com.sports.sales.entity.ReturnRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReturnRequestMapper {

    List<ReturnRequest> selectList(@Param("customerCode") String customerCode,
                                   @Param("status") Integer status,
                                   @Param("type") Integer type,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    Long selectCount(@Param("customerCode") String customerCode,
                     @Param("status") Integer status,
                     @Param("type") Integer type);

    ReturnRequest selectById(@Param("id") Long id);

    int insert(ReturnRequest record);

    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("adminRemark") String adminRemark,
                     @Param("handledBy") String handledBy,
                     @Param("refundAmount") java.math.BigDecimal refundAmount);

    int complete(@Param("id") Long id);

    /** 检查某订单是否已有进行中的退换货申请 */
    int countActiveByOrderId(@Param("orderId") Long orderId);

    /** 检查某订单是否已有已完成的退换货申请 */
    int countCompletedByOrderId(@Param("orderId") Long orderId);
}
