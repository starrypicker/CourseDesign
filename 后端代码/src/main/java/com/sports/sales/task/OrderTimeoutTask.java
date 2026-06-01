package com.sports.sales.task;

import com.sports.sales.dto.OrderQueryDTO;
import com.sports.sales.entity.Orders;
import com.sports.sales.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderMapper orderMapper;

    @Scheduled(cron = "0 */30 * * * ?")
    public void checkOrderTimeout() {
        log.info("====== 开始检查超时订单 ======");
        OrderQueryDTO query = new OrderQueryDTO();
        query.setOrderStatus(0);
        query.setPageNum(0);
        query.setPageSize(1000);
        List<Orders> pendingOrders = orderMapper.selectList(query);

        LocalDateTime now = LocalDateTime.now();
        for (Orders order : pendingOrders) {
            long hours = ChronoUnit.HOURS.between(order.getOrderDate(), now);
            if (hours >= 24) {
                cancelOrderAsync(order.getOrderId()).thenAccept(result -> {
                    if (result) {
                        log.info("超时订单已自动取消: orderId={}", order.getOrderId());
                    }
                });
            }
        }
        log.info("====== 超时订单检查完成 ======");
    }

    @Async("taskExecutor")
    public CompletableFuture<Boolean> cancelOrderAsync(Long orderId) {
        try {
            int result = orderMapper.updateOrderStatus(orderId, 3);
            return CompletableFuture.completedFuture(result > 0);
        } catch (Exception e) {
            log.error("取消超时订单失败: orderId={}", orderId, e);
            return CompletableFuture.completedFuture(false);
        }
    }
}
