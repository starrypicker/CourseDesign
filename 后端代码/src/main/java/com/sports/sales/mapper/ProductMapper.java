package com.sports.sales.mapper;

import com.sports.sales.entity.Product;
import com.sports.sales.dto.ProductQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProductMapper {

    List<Product> selectList(ProductQueryDTO query);

    Long selectCount(ProductQueryDTO query);

    Product selectByCode(@Param("productCode") String productCode);

    int insert(Product product);

    int updateByCode(Product product);

    int deleteByCode(@Param("productCode") String productCode);

    int updateStock(@Param("productCode") String productCode, @Param("quantity") Integer quantity);

    List<Product> selectLowStock();
}
