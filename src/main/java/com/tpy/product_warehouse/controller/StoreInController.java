package com.tpy.product_warehouse.controller;

import com.tpy.product_warehouse.api.CommonsResult;
import com.tpy.product_warehouse.api.ResponseResult;
import com.tpy.product_warehouse.service.InOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "入仓")
@RestController
@RequestMapping("product_warehouse")
public class StoreInController {

    @Resource
    private InOrderService inOrderService;

    @ApiOperation(value="查询待入仓的订单")
    @GetMapping("in_order_list")
    public ResponseResult findAllPendingOrder(){
        return inOrderService.findAllPendingOrder();
    }

    @ApiOperation(value="查询入仓单布号（条码）明细", notes = "根据缸号查询")
    @ApiImplicitParam(name = "cardNo", value = "缸号",paramType = "query",required = true,dataType = "String")
    @GetMapping("in_order_detail")
    public ResponseResult findInOrderDetailByCardNo(String cardNo){
        return inOrderService.findInOrderDetailByCardNo(cardNo);
    }

}
