package com.sports.sales.service.impl;

import com.sports.sales.entity.Manufacturer;
import com.sports.sales.mapper.ManufacturerMapper;
import com.sports.sales.mapper.ProductMapper;
import com.sports.sales.mapper.PurchaseRecordMapper;
import com.sports.sales.service.ManufacturerService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ManufacturerServiceImpl implements ManufacturerService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ManufacturerServiceImpl.class);

    private final ManufacturerMapper manufacturerMapper;
    private final ProductMapper productMapper;
    private final PurchaseRecordMapper purchaseRecordMapper;

    public ManufacturerServiceImpl(ManufacturerMapper manufacturerMapper,
                                   ProductMapper productMapper,
                                   PurchaseRecordMapper purchaseRecordMapper) {
        this.manufacturerMapper = manufacturerMapper;
        this.productMapper = productMapper;
        this.purchaseRecordMapper = purchaseRecordMapper;
    }

    @Override
    public List<Manufacturer> list() {
        return manufacturerMapper.selectList();
    }

    @Override
    @Cacheable(value = "manufacturer", key = "#manufacturerCode", unless = "#result == null")
    public Manufacturer getByCode(String manufacturerCode) {
        return manufacturerMapper.selectByCode(manufacturerCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean add(Manufacturer manufacturer) {
        log.info("添加厂家, manufacturerCode={}, manufacturerName={}", manufacturer.getManufacturerCode(), manufacturer.getManufacturerName());
        boolean result = manufacturerMapper.insert(manufacturer) > 0;
        if (result) {
            log.info("厂家添加成功, manufacturerCode={}", manufacturer.getManufacturerCode());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "manufacturer", key = "#manufacturer.manufacturerCode")
    public boolean update(Manufacturer manufacturer) {
        log.info("更新厂家, manufacturerCode={}", manufacturer.getManufacturerCode());
        return manufacturerMapper.updateByCode(manufacturer) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "manufacturer", key = "#manufacturerCode")
    public boolean delete(String manufacturerCode) {
        log.info("删除厂家, manufacturerCode={}", manufacturerCode);
        // 检查是否有关联商品
        int productCount = productMapper.countByManufacturerCode(manufacturerCode);
        if (productCount > 0) {
            throw new RuntimeException("该厂家下还有 " + productCount + " 个商品，请先删除或转移商品后再删除厂家");
        }
        // 检查是否有关联进货记录
        int purchaseCount = purchaseRecordMapper.countByManufacturerCode(manufacturerCode);
        if (purchaseCount > 0) {
            throw new RuntimeException("该厂家还有 " + purchaseCount + " 条进货记录，请先清理后再删除厂家");
        }
        boolean result = manufacturerMapper.deleteByCode(manufacturerCode) > 0;
        if (result) {
            log.info("厂家删除成功, manufacturerCode={}", manufacturerCode);
        }
        return result;
    }
}
