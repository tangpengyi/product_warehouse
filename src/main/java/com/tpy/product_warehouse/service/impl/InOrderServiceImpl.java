package com.tpy.product_warehouse.service.impl;

import com.tpy.product_warehouse.dao.InOrderDao;
import com.tpy.product_warehouse.api.CommonsResult;
import com.tpy.product_warehouse.api.ResponseResult;
import com.tpy.product_warehouse.service.InOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

@Slf4j
@Service
public class InOrderServiceImpl implements InOrderService {

    //订单号
    private final static int ORDERNO_STYPE = 1;

    //缸号
    private final static int CARDNO_STYPE = 2;

    //布号
    private final static int BARCODE_STYPE = 3;

    @Autowired
    private InOrderDao inOrderDao;

    @Override
    public ResponseResult findAllPendingOrder() {
        ArrayList<Map> pendingOrder = null;
        try {
            pendingOrder = inOrderDao.findAllPendingOrder();
        } catch (SQLException e) {
            log.error("查询带入仓订单出错");
        }
        if(pendingOrder != null){
            return CommonsResult.getSuccessResult("查询成功",pendingOrder);
        }
        return CommonsResult.getFialResult("系统异常");
    }

    @Override
    public ResponseResult findInOrderDetailByCardNo(String orderNo) {
        ArrayList<Map> list = null;
        try {
            list = inOrderDao.findInOrderDetailByOrderNo(orderNo);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if(list != null){
            return CommonsResult.getSuccessResult("查询成功",list);
        }
        return CommonsResult.getFialResult("系统异常");
    }

    @Override
    public ResponseResult findPendingOrderByNo(int paramStype, String param) {

        Map map = null;
        switch (paramStype){
            case ORDERNO_STYPE:
                log.info("根据订单查询入仓单");
                try {
                    map = inOrderDao.findWarehouseReceiptByOrderNo(param);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            case CARDNO_STYPE:
                try {
                    map = inOrderDao.findWarehouseReceiptByCardNo(param);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                log.info("根据缸号查询入仓单");
                break;
            case BARCODE_STYPE:
                try {
                    map = inOrderDao.findWarehouseReceiptByBarCode(param);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                log.info("根据布号（条码）查询入仓单");
                break;
        }

        if(map == null){
            return CommonsResult.getFialResult("系统异常，查询失败");
        }
        return CommonsResult.getSuccessResult("查询成功",map);
    }
}
