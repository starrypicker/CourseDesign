package com.sports.sales.mapper;

import com.sports.sales.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OrderItemMapper {

    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);

    int insertBatch(@Param("items") List<OrderItem> items);

    int deleteByOrderId(@Param("orderId") Long orderId);
}
