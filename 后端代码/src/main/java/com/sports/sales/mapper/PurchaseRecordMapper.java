package com.sports.sales.mapper;

import com.sports.sales.entity.PurchaseRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PurchaseRecordMapper {

    List<PurchaseRecord> selectList();

    PurchaseRecord selectById(@Param("id") Long id);

    int insert(PurchaseRecord record);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
