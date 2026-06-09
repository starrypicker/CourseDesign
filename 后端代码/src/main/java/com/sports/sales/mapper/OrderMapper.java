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

    /** 今日订单数 */
    long selectTodayOrderCount();

    /** 今日销售额 */
    java.math.BigDecimal selectTodaySales();

    /** 待处理订单数（待确认） */
    long selectPendingOrderCount();

    /** 总顾客数 */
    long selectCustomerCount();
}
