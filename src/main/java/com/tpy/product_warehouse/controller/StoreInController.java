package com.tpy.product_warehouse.controller;

import com.tpy.product_warehouse.api.ResponseResult;
import com.tpy.product_warehouse.service.InOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.Map;

@Api(tags = "成品入仓")
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

    @ApiOperation(value="查询入仓单布号（条码）明细", notes = "根据订单号查询")
    @ApiImplicitParam(name = "orderNo", value = "订单号",paramType = "query",required = true,dataType = "String")
    @GetMapping("in_order_detail")
    public ResponseResult findInOrderDetailByOrderNo(String orderNo){
        return inOrderService.findInOrderDetailByOrderNo(orderNo);
    }

    @ApiOperation(value="根据布号或订单号或者缸号查询入仓信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "paramStype", value = "参数类型",paramType = "query",required = true,dataType = "Integer"),
            @ApiImplicitParam(name = "param", value = "参数",paramType = "query",required = true,dataType = "String")
    })
    @GetMapping("in_order_detail_params")
    public ResponseResult findPendingOrderByNo(Integer paramStype,String param) throws SQLException {
        return inOrderService.findPendingOrderByNo(paramStype,param);
    }

    @ApiOperation(value="保存入仓信息")
    @PostMapping("save_in_order")
    public ResponseResult addInStore(@RequestBody Map<String,Object> params) throws SQLException {
        params.put("loginName","唐鹏翼");
        return inOrderService.addInStore(params);
    }
}
