package com.sports.sales.task;

import com.sports.sales.dto.OrderQueryDTO;
import com.sports.sales.entity.Orders;
import com.sports.sales.mapper.OrderMapper;
import com.sports.sales.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    @Scheduled(cron = "0 */30 * * * ?")
    public void checkOrderTimeout() {
        log.info("====== 开始检查超时订单 ======");
        OrderQueryDTO query = new OrderQueryDTO();
        query.setOrderStatus(0);
        query.setPageNum(1);
        query.setPageSize(1000);
        List<Orders> pendingOrders = orderMapper.selectList(query);

        LocalDateTime now = LocalDateTime.now();
        for (Orders order : pendingOrders) {
            if (order.getOrderDate() == null) continue;
            long hours = ChronoUnit.HOURS.between(order.getOrderDate(), now);
            if (hours >= 24) {
                try {
                    orderService.cancelOrder(order.getOrderId());
                    log.info("超时订单已自动取消: orderId={}", order.getOrderId());
                } catch (Exception e) {
                    log.error("取消超时订单失败: orderId={}", order.getOrderId(), e);
                }
            }
        }
        log.info("====== 超时订单检查完成 ======");
    }
}
