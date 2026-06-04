package com.sports.sales.service.impl;

import com.sports.sales.common.PageResult;
import com.sports.sales.dto.OrderCreateDTO;
import com.sports.sales.dto.OrderQueryDTO;
import com.sports.sales.entity.OrderItem;
import com.sports.sales.entity.Orders;
import com.sports.sales.entity.Product;
import com.sports.sales.mapper.OrderItemMapper;
import com.sports.sales.mapper.OrderMapper;
import com.sports.sales.mapper.ProductMapper;
import com.sports.sales.service.OrderService;
import com.sports.sales.util.CryptoUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OrderServiceImpl implements OrderService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final RedissonClient redissonClient;
    private final CryptoUtil cryptoUtil;

    public OrderServiceImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                            ProductMapper productMapper, RedissonClient redissonClient,
                            CryptoUtil cryptoUtil) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.redissonClient = redissonClient;
        this.cryptoUtil = cryptoUtil;
    }

    @Override
    public PageResult<Orders> list(OrderQueryDTO query) {
        query.setPageNum((query.getPageNum() - 1) * query.getPageSize());
        Long total = orderMapper.selectCount(query);
        List<Orders> rows = orderMapper.selectList(query);
        rows.forEach(order -> {
            order.setItems(orderItemMapper.selectByOrderId(order.getOrderId()));
            decryptOrderSensitiveData(order);
        });
        return new PageResult<>(total, rows, query.getPageNum() / query.getPageSize() + 1, query.getPageSize());
    }

    @Override
    @Cacheable(value = "order", key = "#orderId")
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
        String lockKey = "lock:order:create:" + dto.getCustomerCode();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

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
            boolean canSupply = true;

            for (OrderCreateDTO.OrderItemDTO itemDTO : dto.getItems()) {
                Product product = productMapper.selectByCode(itemDTO.getProductCode());
                if (product == null) {
                    log.warn("商品不存在: {}", itemDTO.getProductCode());
                    throw new RuntimeException("商品不存在: " + itemDTO.getProductCode());
                }
                if (product.getStockQuantity() < itemDTO.getQuantity()) {
                    canSupply = false;
                    log.warn("商品库存不足: productCode={}, 需要={}, 当前={}", itemDTO.getProductCode(), itemDTO.getQuantity(), product.getStockQuantity());
                }

                OrderItem item = new OrderItem();
                item.setProductCode(itemDTO.getProductCode());
                item.setManufacturerCode(product.getManufacturerCode());
                item.setQuantity(itemDTO.getQuantity());
                item.setUnitPrice(product.getUnitPrice());
                item.setTotalAmount(product.getUnitPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
                items.add(item);

                totalAmount = totalAmount.add(item.getTotalAmount());
                totalWeight = totalWeight.add(product.getWeight().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
            }

            order.setCanSupply(canSupply ? 1 : 0);
            order.setTotalAmount(totalAmount);
            order.setTotalWeight(totalWeight);
            order.setShippingFee(calculateShippingFee(totalWeight));
            order.setShippingRequirement(dto.getShippingRequirement());

            orderMapper.insert(order);

            for (OrderItem item : items) {
                item.setOrderId(order.getOrderId());
            }
            orderItemMapper.insertBatch(items);

            if (canSupply) {
                for (OrderCreateDTO.OrderItemDTO itemDTO : dto.getItems()) {
                    productMapper.updateStock(itemDTO.getProductCode(), -itemDTO.getQuantity());
                }
            }

            log.info("订单创建成功, orderId={}, canSupply={}, totalAmount={}", order.getOrderId(), canSupply, totalAmount);
            return order.getOrderId();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("创建订单被中断", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
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
        if (order.getCanSupply() == 1) {
            List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
            for (OrderItem item : items) {
                productMapper.updateStock(item.getProductCode(), item.getQuantity());
                log.info("库存回滚, productCode={}, quantity={}", item.getProductCode(), item.getQuantity());
            }
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
        if (order.getPaymentStatus() != 0) {
            throw new RuntimeException("订单付款状态不正确");
        }
        order.setPaymentStatus(1);
        order.setPaymentMethod(paymentMethod);
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
        orders.forEach(o -> o.setItems(orderItemMapper.selectByOrderId(o.getOrderId())));
        return orders;
    }

    @Override
    public List<Orders> getUnshippedOrders() {
        List<Orders> orders = orderMapper.selectUnshippedOrders();
        orders.forEach(o -> o.setItems(orderItemMapper.selectByOrderId(o.getOrderId())));
        return orders;
    }

    @Override
    public List<Orders> getCompletedOrders() {
        List<Orders> orders = orderMapper.selectCompletedOrders();
        orders.forEach(o -> o.setItems(orderItemMapper.selectByOrderId(o.getOrderId())));
        return orders;
    }

    private BigDecimal calculateShippingFee(BigDecimal totalWeight) {
        BigDecimal baseFee = BigDecimal.valueOf(10);
        BigDecimal extraFee = totalWeight.subtract(BigDecimal.valueOf(5))
                .max(BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(2));
        return baseFee.add(extraFee);
    }

    private void decryptOrderSensitiveData(Orders order) {
        if (order.getRecipientPhone() != null) {
            order.setRecipientPhone(cryptoUtil.decrypt(order.getRecipientPhone()));
        }
    }
}
