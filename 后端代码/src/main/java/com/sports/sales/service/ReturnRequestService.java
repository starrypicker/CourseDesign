package com.sports.sales.service;

import com.sports.sales.common.PageResult;
import com.sports.sales.entity.ReturnRequest;

public interface ReturnRequestService {

    PageResult<ReturnRequest> list(String customerCode, Integer status, Integer type, int pageNum, int pageSize);

    ReturnRequest getById(Long id);

    Long create(ReturnRequest record);

    boolean approve(Long id, String adminRemark, java.math.BigDecimal refundAmount, String handledBy);

    boolean reject(Long id, String adminRemark, String handledBy);

    boolean complete(Long id);
}
