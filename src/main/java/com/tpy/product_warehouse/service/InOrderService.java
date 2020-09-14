package com.tpy.product_warehouse.service;

import com.tpy.product_warehouse.api.ResponseResult;

import java.sql.SQLException;

public interface InOrderService {
    public ResponseResult findAllPendingOrder();
    public ResponseResult findInOrderDetailByCardNo(String orderNo);
    public ResponseResult findPendingOrderByNo(int paramStype,String param) throws SQLException;

}
