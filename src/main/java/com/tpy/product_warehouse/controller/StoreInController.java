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
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

@Api(tags = "成品入仓")
@RestController
@RequestMapping("product_warehouse/store_in")
public class StoreInController {

    @Resource
    private InOrderService inOrderService;

    @ApiOperation(value="查询待入仓的订单")
    @GetMapping("in_order_list")
    public ResponseResult findAllPendingOrder() throws SQLException {
        return inOrderService.findAllPendingOrder();
    }

    @ApiOperation(value="查询入仓单布号（条码）明细", notes = "根据订单号查询")
    @PostMapping("in_order_detail")
    public ResponseResult findInOrderDetailByOrderNo(@RequestBody List<Object> orderNos) throws SQLException {
        return inOrderService.findInOrderDetailByOrderNo(orderNos);
    }

    @ApiOperation(value="根据布号或订单号或者缸号查询入仓信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "paramStype", value = "参数类型",paramType = "query",required = true,dataType = "int"),
            @ApiImplicitParam(name = "param", value = "参数",paramType = "query",required = true,dataType = "String")
    })
    @GetMapping("in_order_detail_params")
    public ResponseResult findPendingOrderByNo(Integer paramStype,String param) throws SQLException {
        return inOrderService.findPendingOrderByNo(paramStype,param);
    }

    @ApiOperation(value="保存入仓信息")
    @PostMapping("save_in_order")
    public ResponseResult addInStore(@RequestBody List<Map<String,Object>> params) throws SQLException, ClassNotFoundException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return inOrderService.addInStore(params);
    }

    @ApiOperation(value="根据订单号查询订单信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "paramStype", value = "参数类型",paramType = "query",required = true,dataType = "int"),
            @ApiImplicitParam(name = "param", value = "参数",paramType = "query",required = true,dataType = "String")
    })
    @GetMapping("find_in_order")
    public ResponseResult getStoreInOrder(String param,int paramStype) throws SQLException, ClassNotFoundException {

        return inOrderService.getStoreInOrderByParam(param,paramStype);
    }
}
