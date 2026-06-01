package com.sports.sales.mapper;

import com.sports.sales.entity.Manufacturer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ManufacturerMapper {

    List<Manufacturer> selectList();

    Manufacturer selectByCode(@Param("manufacturerCode") String manufacturerCode);

    int insert(Manufacturer manufacturer);

    int updateByCode(Manufacturer manufacturer);

    int deleteByCode(@Param("manufacturerCode") String manufacturerCode);
}
