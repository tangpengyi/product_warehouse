package com.tpy.product_warehouse.service;

import com.tpy.product_warehouse.api.ResponseResult;

import java.sql.SQLException;

public interface OutOrderService {

    public ResponseResult getAllOutStore() throws SQLException, ClassNotFoundException;

    public ResponseResult getOutStoreInfo(String uGUID);

    public ResponseResult getOutStoreByBarCode(String uGUID);
}
