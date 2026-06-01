package com.sports.sales.service.impl;

import com.sports.sales.common.PageResult;
import com.sports.sales.dto.ProductQueryDTO;
import com.sports.sales.entity.Product;
import com.sports.sales.mapper.ProductMapper;
import com.sports.sales.service.ProductService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProductServiceImpl implements ProductService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductMapper productMapper;
    private final RedissonClient redissonClient;

    public ProductServiceImpl(ProductMapper productMapper, RedissonClient redissonClient) {
        this.productMapper = productMapper;
        this.redissonClient = redissonClient;
    }

    @Override
    public PageResult<Product> list(ProductQueryDTO query) {
        query.setPageNum((query.getPageNum() - 1) * query.getPageSize());
        Long total = productMapper.selectCount(query);
        List<Product> rows = productMapper.selectList(query);
        return new PageResult<>(total, rows, query.getPageNum() / query.getPageSize() + 1, query.getPageSize());
    }

    @Override
    @Cacheable(value = "product", key = "#productCode")
    public Product getByCode(String productCode) {
        return productMapper.selectByCode(productCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product", key = "#product.productCode")
    public boolean add(Product product) {
        product.setStatus(1);
        return productMapper.insert(product) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product", key = "#product.productCode")
    public boolean update(Product product) {
        return productMapper.updateByCode(product) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product", key = "#productCode")
    public boolean delete(String productCode) {
        return productMapper.deleteByCode(productCode) > 0;
    }

    @Override
    @Cacheable(value = "lowStockProducts", key = "'all'")
    public List<Product> getLowStockProducts() {
        return productMapper.selectLowStock();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"product", "lowStockProducts"}, key = "#productCode")
    public boolean replenishStock(String productCode, Integer quantity) {
        String lockKey = "lock:stock:" + productCode;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!locked) {
                log.error("获取分布式锁失败, productCode={}", productCode);
                throw new RuntimeException("系统繁忙，请稍后重试");
            }
            int result = productMapper.updateStock(productCode, quantity);
            if (result > 0) {
                log.info("库存补充成功, productCode={}, quantity={}", productCode, quantity);
            }
            return result > 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("库存补充操作被中断", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
