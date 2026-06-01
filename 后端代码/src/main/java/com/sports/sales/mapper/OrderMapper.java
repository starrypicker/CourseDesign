package com.sports.sales.mapper;

import com.sports.sales.entity.Orders;
import com.sports.sales.dto.OrderQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OrderMapper {

    List<Orders> selectList(OrderQueryDTO query);

    Long selectCount(OrderQueryDTO query);

    Orders selectById(@Param("orderId") Long orderId);

    int insert(Orders orders);

    int updateById(Orders orders);

    int updateOrderStatus(@Param("orderId") Long orderId, @Param("orderStatus") Integer orderStatus);

    int updatePaymentStatus(@Param("orderId") Long orderId, @Param("paymentStatus") Integer paymentStatus);

    int updateShippingStatus(@Param("orderId") Long orderId, @Param("shippingStatus") Integer shippingStatus);

    List<Orders> selectUnpaidOrders();

    List<Orders> selectUnshippedOrders();

    List<Orders> selectCompletedOrders();
}
