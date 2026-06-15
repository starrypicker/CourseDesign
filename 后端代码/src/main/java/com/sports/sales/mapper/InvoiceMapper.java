package com.sports.sales.mapper;

import com.sports.sales.entity.Invoice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InvoiceMapper {

    List<Invoice> selectList(@Param("customerCode") String customerCode,
                             @Param("status") Integer status,
                             @Param("offset") int offset,
                             @Param("limit") int limit);

    Long selectCount(@Param("customerCode") String customerCode,
                     @Param("status") Integer status);

    Invoice selectById(@Param("id") Long id);

    Invoice selectByOrderId(@Param("orderId") Long orderId);

    int insert(Invoice record);

    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("invoiceNo") String invoiceNo);

    /** 作废/取消发票，状态设为2 */
    int cancelInvoice(@Param("id") Long id);
}
