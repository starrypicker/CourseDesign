package com.sports.sales.service;

import com.sports.sales.common.PageResult;
import com.sports.sales.dto.OrderCreateDTO;
import com.sports.sales.dto.OrderQueryDTO;
import com.sports.sales.entity.Orders;
import java.util.List;

public interface OrderService {

    PageResult<Orders> list(OrderQueryDTO query);

    Orders getById(Long orderId);

    Long createOrder(OrderCreateDTO dto);

    boolean confirmOrder(Long orderId);

    boolean shipOrder(Long orderId);

    boolean cancelOrder(Long orderId);

    boolean payOrder(Long orderId, String paymentMethod);

    boolean completeOrder(Long orderId);

    List<Orders> getUnpaidOrders();

    List<Orders> getUnshippedOrders();

    List<Orders> getCompletedOrders();

    java.util.Map<String, Object> getDashboardStats();
}
