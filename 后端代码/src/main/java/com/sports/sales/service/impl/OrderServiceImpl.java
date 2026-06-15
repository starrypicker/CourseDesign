package com.sports.sales.service.impl;

import com.sports.sales.common.PageResult;
import com.sports.sales.dto.OrderCreateDTO;
import com.sports.sales.dto.OrderQueryDTO;
import com.sports.sales.entity.OrderItem;
import com.sports.sales.entity.Orders;
import com.sports.sales.entity.Product;
import com.sports.sales.mapper.CustomerMapper;
import com.sports.sales.mapper.OrderItemMapper;
import com.sports.sales.mapper.OrderMapper;
import com.sports.sales.mapper.ProductMapper;
import com.sports.sales.service.OrderService;
import com.sports.sales.util.CryptoUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class);

    // 运费计算参数
    private static final java.math.BigDecimal SHIPPING_BASE_FEE = java.math.BigDecimal.valueOf(10);
    private static final java.math.BigDecimal SHIPPING_WEIGHT_THRESHOLD = java.math.BigDecimal.valueOf(5);
    private static final java.math.BigDecimal SHIPPING_PER_KG_FEE = java.math.BigDecimal.valueOf(2);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final CustomerMapper customerMapper;
    private final RedissonClient redissonClient;
    private final CryptoUtil cryptoUtil;
    private final CacheManager cacheManager;

    public OrderServiceImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                            ProductMapper productMapper, CustomerMapper customerMapper,
                            RedissonClient redissonClient, CryptoUtil cryptoUtil,
                            CacheManager cacheManager) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.customerMapper = customerMapper;
        this.redissonClient = redissonClient;
        this.cryptoUtil = cryptoUtil;
        this.cacheManager = cacheManager;
    }

    @Override
    public PageResult<Orders> list(OrderQueryDTO query) {
        Long total = orderMapper.selectCount(query);
        List<Orders> rows = orderMapper.selectList(query);
        rows.forEach(order -> {
            order.setItems(orderItemMapper.selectByOrderId(order.getOrderId()));
            decryptOrderSensitiveData(order);
        });
        return new PageResult<>(total, rows, query.getPageNum(), query.getPageSize());
    }

    @Override
    @Cacheable(value = "order", key = "#orderId", unless = "#result == null")
    public Orders getById(Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order != null) {
            order.setItems(orderItemMapper.selectByOrderId(orderId));
            decryptOrderSensitiveData(order);
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateDTO dto) {
        // 按商品编码排序后逐商品加锁，防止死锁和超卖
        List<String> sortedProductCodes = dto.getItems().stream()
                .map(OrderCreateDTO.OrderItemDTO::getProductCode)
                .distinct().sorted().collect(Collectors.toList());

        List<RLock> locks = new ArrayList<>();
        for (String pc : sortedProductCodes) {
            locks.add(redissonClient.getLock("lock:stock:" + pc));
        }

        boolean syncRegistered = false;
        try {
            // 统一获取所有锁（逐个获取，失败时 finally 释放已获取的锁）
            for (RLock rl : locks) {
                boolean acquired = false;
                try {
                    acquired = rl.tryLock(10, 30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("创建订单被中断", e);
                }
                if (!acquired) {
                    throw new RuntimeException("系统繁忙，请稍后重试");
                }
            }

            // 注册事务同步回调：事务提交后再释放锁，防止锁提前释放导致超卖
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    releaseLocks(locks);
                }
            });
            syncRegistered = true;

            log.info("开始创建订单, customerCode={}, 商品数={}", dto.getCustomerCode(), dto.getItems().size());

            Orders order = new Orders();
            order.setOrderDate(LocalDateTime.now());
            order.setCustomerCode(dto.getCustomerCode());
            order.setShippingRequirement(dto.getShippingRequirement());
            order.setPaymentStatus(0);
            order.setShippingStatus(0);
            order.setOrderStatus(0);
            order.setRecipientName(dto.getRecipientName());
            order.setRecipientAddress(dto.getRecipientAddress());
            order.setRecipientPhone(cryptoUtil.encrypt(dto.getRecipientPhone()));
            order.setPaymentMethod(dto.getPaymentMethod());
            order.setRemark(dto.getRemark());

            List<OrderItem> items = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal totalWeight = BigDecimal.ZERO;

            for (OrderCreateDTO.OrderItemDTO itemDTO : dto.getItems()) {
                Product product = productMapper.selectByCode(itemDTO.getProductCode());
                if (product == null) {
                    throw new RuntimeException("商品不存在: " + itemDTO.getProductCode());
                }
                if (product.getStatus() == null || product.getStatus() != 1) {
                    String msg = String.format("商品[%s]已下架，无法购买", product.getProductName());
                    log.warn(msg);
                    throw new RuntimeException(msg);
                }
                if (product.getStockQuantity() == null || product.getStockQuantity() < itemDTO.getQuantity()) {
                    String msg = String.format("商品[%s]库存不足，当前库存: %d，需要: %d，订单将标记为缺货",
                            product.getProductName(), product.getStockQuantity() != null ? product.getStockQuantity() : 0, itemDTO.getQuantity());
                    log.warn(msg);
                    // 库存不足时标记为缺货订单，不抛异常，允许创建
                    order.setCanSupply(0);
                }

                OrderItem item = new OrderItem();
                item.setProductCode(itemDTO.getProductCode());
                item.setManufacturerCode(product.getManufacturerCode());
                item.setQuantity(itemDTO.getQuantity());
                item.setUnitPrice(product.getUnitPrice());
                BigDecimal unitPrice = product.getUnitPrice() != null ? product.getUnitPrice() : BigDecimal.ZERO;
                item.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
                items.add(item);

                totalAmount = totalAmount.add(item.getTotalAmount());
                totalWeight = totalWeight.add(
                        product.getWeight() != null ? product.getWeight().multiply(BigDecimal.valueOf(itemDTO.getQuantity())) : BigDecimal.ZERO);
            }

            // canSupply 可能在前面检查库存不足时已被设为0，此处仅当仍有库存时设为1
            if (order.getCanSupply() == null) {
                order.setCanSupply(1);
            }
            order.setTotalAmount(totalAmount);
            order.setTotalWeight(totalWeight);
            order.setShippingFee(calculateShippingFee(totalWeight));

            orderMapper.insert(order);

            for (OrderItem item : items) {
                item.setOrderId(order.getOrderId());
            }
            orderItemMapper.insertBatch(items);

            // 仅可供货的商品才扣减库存，缺货商品保留库存供后续进货
            for (OrderCreateDTO.OrderItemDTO itemDTO : dto.getItems()) {
                Product p = productMapper.selectByCode(itemDTO.getProductCode());
                if (p != null && p.getStockQuantity() != null && p.getStockQuantity() >= itemDTO.getQuantity()) {
                    productMapper.updateStock(itemDTO.getProductCode(), -itemDTO.getQuantity());
                    evictProductCacheByCode(itemDTO.getProductCode());
                }
            }
            evictLowStockCache();

            log.info("订单创建成功, orderId={}, totalAmount={}", order.getOrderId(), totalAmount);
            return order.getOrderId();
        } finally {
            // 锁获取阶段失败时在此兜底释放；正常流程由事务同步回调负责释放
            if (!syncRegistered) {
                releaseLocks(locks);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "order", key = "#orderId")
    public boolean confirmOrder(Long orderId) {
        log.info("确认订单, orderId={}", orderId);
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getOrderStatus() != 0) {
            throw new RuntimeException("订单状态不允许确认");
        }
        return orderMapper.updateOrderStatus(orderId, 1) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "order", key = "#orderId")
    public boolean shipOrder(Long orderId) {
        log.info("发货操作, orderId={}", orderId);
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getOrderStatus() == 3) {
            throw new RuntimeException("订单已取消，无法发货");
        }
        if (order.getOrderStatus() != 1) {
            throw new RuntimeException("订单未确认，无法发货");
        }
        if (order.getPaymentStatus() != 1) {
            throw new RuntimeException("订单未付款，无法发货");
        }
        if (order.getShippingStatus() != 0) {
            throw new RuntimeException("订单发货状态不正确");
        }
        order.setShippingStatus(1);
        order.setShippingDate(LocalDateTime.now());
        return orderMapper.updateById(order) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "order", key = "#orderId")
    public boolean cancelOrder(Long orderId) {
        log.info("取消订单, orderId={}", orderId);
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getOrderStatus() == 2) {
            throw new RuntimeException("已完成的订单不能取消");
        }
        if (order.getOrderStatus() == 3) {
            throw new RuntimeException("订单已取消，不能重复取消");
        }
        if (order.getShippingStatus() != null && order.getShippingStatus() != 0) {
            throw new RuntimeException("订单已发货，无法取消，请联系客服处理");
        }
        // 已付款订单取消时自动退款（使用专用方法避免覆盖加密字段）
        if (order.getPaymentStatus() != null && order.getPaymentStatus() == 1) {
            orderMapper.updatePaymentStatus(orderId, 2); // 2-已退款
            log.info("订单退款, orderId={}, amount={}", orderId, order.getTotalAmount());
        }
        if (order.getCanSupply() != null && order.getCanSupply() == 1) {
            List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
            for (OrderItem item : items) {
                productMapper.updateStock(item.getProductCode(), item.getQuantity());
                evictProductCacheByCode(item.getProductCode());
                log.info("库存回滚, productCode={}, quantity={}", item.getProductCode(), item.getQuantity());
            }
            evictLowStockCache();
        }
        log.info("订单取消成功, orderId={}", orderId);
        return orderMapper.updateOrderStatus(orderId, 3) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "order", key = "#orderId")
    public boolean payOrder(Long orderId, String paymentMethod) {
        log.info("订单付款, orderId={}, paymentMethod={}", orderId, paymentMethod);
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getOrderStatus() == 3) {
            throw new RuntimeException("订单已取消，无法付款");
        }
        if (order.getOrderStatus() != 1) {
            throw new RuntimeException("订单未确认，无法付款");
        }
        if (order.getPaymentStatus() != 0) {
            throw new RuntimeException("订单付款状态不正确");
        }
        order.setPaymentStatus(1);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentListNo("PAY-" + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8) + "-" + orderId);
        order.setPaymentTime(LocalDateTime.now());
        return orderMapper.updateById(order) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "order", key = "#orderId")
    public boolean completeOrder(Long orderId) {
        log.info("完成订单, orderId={}", orderId);
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getOrderStatus() == 3) {
            throw new RuntimeException("订单已取消，无法完成");
        }
        if (order.getShippingStatus() != 1) {
            throw new RuntimeException("订单未发货，无法完成");
        }
        order.setShippingStatus(2);
        order.setOrderStatus(2);
        return orderMapper.updateById(order) > 0;
    }

    @Override
    public List<Orders> getUnpaidOrders() {
        List<Orders> orders = orderMapper.selectUnpaidOrders();
        orders.forEach(o -> {
            o.setItems(orderItemMapper.selectByOrderId(o.getOrderId()));
            decryptOrderSensitiveData(o);
        });
        return orders;
    }

    @Override
    public List<Orders> getUnshippedOrders() {
        List<Orders> orders = orderMapper.selectUnshippedOrders();
        orders.forEach(o -> {
            o.setItems(orderItemMapper.selectByOrderId(o.getOrderId()));
            decryptOrderSensitiveData(o);
        });
        return orders;
    }

    @Override
    public List<Orders> getCompletedOrders() {
        List<Orders> orders = orderMapper.selectCompletedOrders();
        orders.forEach(o -> {
            o.setItems(orderItemMapper.selectByOrderId(o.getOrderId()));
            decryptOrderSensitiveData(o);
        });
        return orders;
    }

    private BigDecimal calculateShippingFee(BigDecimal totalWeight) {
        BigDecimal extraWeight = totalWeight.subtract(SHIPPING_WEIGHT_THRESHOLD).max(BigDecimal.ZERO);
        BigDecimal extraFee = extraWeight.multiply(SHIPPING_PER_KG_FEE);
        return SHIPPING_BASE_FEE.add(extraFee);
    }

    private void decryptOrderSensitiveData(Orders order) {
        if (order.getRecipientPhone() != null) {
            order.setRecipientPhone(tryDecrypt(order.getRecipientPhone()));
        }
    }

    /**
     * 尝试解密，如果数据是明文（未加密），则直接返回原文
     */
    private String tryDecrypt(String value) {
        try {
            return cryptoUtil.decrypt(value);
        } catch (Exception e) {
            return value;
        }
    }

    @Override
    public java.util.Map<String, Object> getDashboardStats() {
        java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();

        // 今日订单数
        long todayOrders = orderMapper.selectTodayOrderCount();
        stats.put("todayOrders", todayOrders);

        // 今日销售额
        java.math.BigDecimal todaySales = orderMapper.selectTodaySales();
        stats.put("todaySales", todaySales != null ? todaySales : java.math.BigDecimal.ZERO);

        // 待处理订单（待确认 + 未发货）
        long pendingOrders = orderMapper.selectPendingOrderCount();
        stats.put("pendingOrders", pendingOrders);

        // 低库存商品数
        long lowStockCount = productMapper.selectLowStockCount();
        stats.put("lowStockCount", lowStockCount);

        // 总商品数
        long totalProducts = productMapper.selectTotalCount();
        stats.put("totalProducts", totalProducts);

        // 总顾客数
        long totalCustomers = customerMapper.selectTotalCount();
        stats.put("totalCustomers", totalCustomers);

        // 本月订单数
        long monthOrders = orderMapper.selectMonthOrderCount();
        stats.put("monthOrders", monthOrders);

        // 本月销售额
        java.math.BigDecimal monthSales = orderMapper.selectMonthSales();
        stats.put("monthSales", monthSales != null ? monthSales : java.math.BigDecimal.ZERO);

        // 销量排行榜 Top10
        java.util.List<java.util.Map<String, Object>> salesRanking = orderItemMapper.selectSalesRanking();
        stats.put("salesRanking", salesRanking);

        // 累计销量件数
        Long totalSold = orderItemMapper.selectTotalSoldQuantity();
        stats.put("totalSold", totalSold != null ? totalSold : 0L);

        return stats;
    }

    @Override
    public List<Orders> getOutOfStockOrders() {
        List<Orders> orders = orderMapper.selectOutOfStockOrders();
        orders.forEach(o -> {
            o.setItems(orderItemMapper.selectByOrderId(o.getOrderId()));
            decryptOrderSensitiveData(o);
        });
        return orders;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "order", key = "#orderId")
    public boolean markAsSuppliable(Long orderId) {
        log.info("标记订单为可供货, orderId={}", orderId);
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getCanSupply() != null && order.getCanSupply() == 1) {
            throw new RuntimeException("该订单已标记为可供货");
        }
        // 扣减库存
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : items) {
            Product product = productMapper.selectByCode(item.getProductCode());
            if (product == null || product.getStockQuantity() == null || product.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("商品[" + (product != null ? product.getProductName() : item.getProductCode()) + "]库存仍不足，请先进货");
            }
            productMapper.updateStock(item.getProductCode(), -item.getQuantity());
            evictProductCacheByCode(item.getProductCode());
        }
        evictLowStockCache();
        order.setCanSupply(1);
        int rows = orderMapper.updateById(order);
        log.info("订单已标记为可供货, orderId={}", orderId);
        return rows > 0;
    }

    /** 释放锁列表（用于事务同步回调和兜底释放） */
    private void releaseLocks(List<RLock> locks) {
        for (int i = locks.size() - 1; i >= 0; i--) {
            RLock rl = locks.get(i);
            if (rl.isHeldByCurrentThread()) {
                try {
                    rl.unlock();
                } catch (Exception ignored) {
                    // 锁可能已被释放或过期，忽略异常
                }
            }
        }
    }

    /** 精确清除单个商品缓存 */
    private void evictProductCacheByCode(String productCode) {
        Cache productCache = cacheManager.getCache("product");
        if (productCache != null) {
            productCache.evict(productCode);
        }
    }

    /** 清除低库存列表缓存 */
    private void evictLowStockCache() {
        Cache lowStockCache = cacheManager.getCache("lowStockProducts");
        if (lowStockCache != null) {
            lowStockCache.clear();
        }
    }
}
