package com.tpy.product_warehouse.service;

import com.tpy.product_warehouse.api.ResponseResult;

public interface InOrderService {
    public ResponseResult findAllPendingOrder();
    public ResponseResult findInOrderDetailByCardNo(String orderNo);
}
