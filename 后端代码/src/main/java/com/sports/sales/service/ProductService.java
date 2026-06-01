package com.sports.sales.service;

import com.sports.sales.common.PageResult;
import com.sports.sales.dto.ProductQueryDTO;
import com.sports.sales.entity.Product;
import java.util.List;

public interface ProductService {

    PageResult<Product> list(ProductQueryDTO query);

    Product getByCode(String productCode);

    boolean add(Product product);

    boolean update(Product product);

    boolean delete(String productCode);

    List<Product> getLowStockProducts();

    boolean replenishStock(String productCode, Integer quantity);
}
