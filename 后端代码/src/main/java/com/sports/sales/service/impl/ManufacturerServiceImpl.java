package com.sports.sales.service.impl;

import com.sports.sales.entity.Manufacturer;
import com.sports.sales.mapper.ManufacturerMapper;
import com.sports.sales.service.ManufacturerService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ManufacturerServiceImpl implements ManufacturerService {

    private final ManufacturerMapper manufacturerMapper;

    public ManufacturerServiceImpl(ManufacturerMapper manufacturerMapper) {
        this.manufacturerMapper = manufacturerMapper;
    }

    @Override
    public List<Manufacturer> list() {
        return manufacturerMapper.selectList();
    }

    @Override
    @Cacheable(value = "manufacturer", key = "#manufacturerCode")
    public Manufacturer getByCode(String manufacturerCode) {
        return manufacturerMapper.selectByCode(manufacturerCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean add(Manufacturer manufacturer) {
        return manufacturerMapper.insert(manufacturer) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "manufacturer", key = "#manufacturer.manufacturerCode")
    public boolean update(Manufacturer manufacturer) {
        return manufacturerMapper.updateByCode(manufacturer) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "manufacturer", key = "#manufacturerCode")
    public boolean delete(String manufacturerCode) {
        return manufacturerMapper.deleteByCode(manufacturerCode) > 0;
    }
}
