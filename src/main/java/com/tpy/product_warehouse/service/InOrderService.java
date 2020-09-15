package com.tpy.product_warehouse.service;

import com.tpy.product_warehouse.api.ResponseResult;

import java.sql.SQLException;
import java.util.Map;

public interface InOrderService {

    public ResponseResult findAllPendingOrder();

    public ResponseResult findInOrderDetailByOrderNo(String orderNo);

    public ResponseResult findPendingOrderByNo(int paramStype,String param) throws SQLException;

    public ResponseResult addInStore(Map map) throws SQLException;
}
