package com.tpy.product_warehouse.service;

import com.tpy.product_warehouse.api.ResponseResult;

import java.sql.SQLException;
import java.util.Map;

public interface InOrderService {

    public ResponseResult findAllPendingOrder() throws SQLException;

    public ResponseResult findInOrderDetailByOrderNo(String orderNo) throws SQLException;

    public ResponseResult findPendingOrderByNo(int paramStype,String param) throws SQLException;

    public ResponseResult addInStore(Map map) throws SQLException, ClassNotFoundException;
}
