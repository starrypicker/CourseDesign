package com.sports.sales.task;

import com.sports.sales.dto.OrderQueryDTO;
import com.sports.sales.entity.Orders;
import com.sports.sales.mapper.OrderMapper;
import com.sports.sales.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OrderTimeoutTask {

    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final RedissonClient redissonClient;

    public OrderTimeoutTask(OrderMapper orderMapper, OrderService orderService,
                            RedissonClient redissonClient) {
        this.orderMapper = orderMapper;
        this.orderService = orderService;
        this.redissonClient = redissonClient;
    }

    @Scheduled(cron = "0 */30 * * * ?")
    public void checkOrderTimeout() {
        log.info("====== 开始检查超时订单 ======");
        LocalDateTime now = LocalDateTime.now();

        int pageSize = 500;
        int pageNum = 1;
        int totalCancelled = 0;

        while (true) {
            OrderQueryDTO query = new OrderQueryDTO();
            query.setOrderStatus(0);
            query.setPageNum(pageNum);
            query.setPageSize(pageSize);
            List<Orders> pendingOrders = orderMapper.selectList(query);
            if (pendingOrders.isEmpty()) {
                break;
            }

            for (Orders order : pendingOrders) {
                if (order.getOrderDate() == null) continue;
                long hours = ChronoUnit.HOURS.between(order.getOrderDate(), now);
                if (hours >= 24) {
                    // 加分布式锁防止与用户手动取消并发冲突
                    RLock lock = redissonClient.getLock("lock:order:cancel:" + order.getOrderId());
                    try {
                        if (lock.tryLock(2, 10, TimeUnit.SECONDS)) {
                            try {
                                orderService.cancelOrder(order.getOrderId());
                                totalCancelled++;
                                log.info("超时订单已自动取消: orderId={}", order.getOrderId());
                            } catch (Exception e) {
                                log.error("取消超时订单失败: orderId={}", order.getOrderId(), e);
                            } finally {
                                if (lock.isHeldByCurrentThread()) {
                                    try { lock.unlock(); } catch (Exception ignored) {}
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("超时订单处理被中断: orderId={}", order.getOrderId());
                        break;
                    }
                }
            }

            // 如果返回数量小于pageSize，说明已经是最后一页
            if (pendingOrders.size() < pageSize) {
                break;
            }
            pageNum++;
        }

        log.info("====== 超时订单检查完成, 共取消{}笔订单 ======", totalCancelled);
    }
}
