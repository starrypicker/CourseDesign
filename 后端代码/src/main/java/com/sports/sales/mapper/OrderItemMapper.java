package com.sports.sales.mapper;

import com.sports.sales.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderItemMapper {

    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);

    int insertBatch(@Param("items") List<OrderItem> items);

    int deleteByOrderId(@Param("orderId") Long orderId);

    /** 销量排行榜 Top10：已完成订单中按销量降序 */
    List<Map<String, Object>> selectSalesRanking();

    /** 已完成订单总销量件数 */
    Long selectTotalSoldQuantity();
}
