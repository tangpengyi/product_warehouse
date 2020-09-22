package com.tpy.product_warehouse.controller;

import com.tpy.product_warehouse.api.ResponseResult;
import com.tpy.product_warehouse.service.OutOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

@Api(tags = "成品出仓")
@RestController
@RequestMapping("product_warehouse/store_out")
public class StoreOutController {

    @Autowired
    private OutOrderService outOrderService;

    @ApiOperation(value="查询所有待出仓的订单")
    @GetMapping("getAllStoreOutOrder")
    public ResponseResult getAllOutStore() throws SQLException, ClassNotFoundException {
        return outOrderService.getAllOutStore();
    }

    @ApiOperation(value="根据GUID查询出库订单信息")
    @GetMapping("getOutOrderInfo")
    public ResponseResult getOutStoreInfo(String uGUID){
        return outOrderService.getOutStoreInfo(uGUID);
    }

    @ApiOperation(value="根据GUID查询出库订单信息")
    @GetMapping("getOutOrderBarCode")
    public ResponseResult getOutStoreBarCode(String uGUID){
        return outOrderService.getOutStoreByBarCode(uGUID);
    }
}
