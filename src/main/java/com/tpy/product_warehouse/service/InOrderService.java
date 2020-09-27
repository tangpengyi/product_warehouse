package com.tpy.product_warehouse.service;

import com.tpy.product_warehouse.api.ResponseResult;
import springfox.documentation.service.ResponseMessage;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface InOrderService {

    public ResponseResult getStoreInOrderByParam(String param,int paramStype) throws SQLException, ClassNotFoundException;

    public ResponseResult findAllPendingOrder() throws SQLException;

    public ResponseResult findInOrderDetailByOrderNo(List<Object> orderNos) throws SQLException;

    public ResponseResult findPendingOrderByNo(int paramStype,String param) throws SQLException;

    public ResponseResult addInStore(List<Map<String,Object>> list) throws SQLException, ClassNotFoundException;
}
