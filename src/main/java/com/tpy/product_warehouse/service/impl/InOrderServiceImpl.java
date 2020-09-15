package com.tpy.product_warehouse.service.impl;

import com.tpy.product_warehouse.dao.InOrderDao;
import com.tpy.product_warehouse.api.CommonsResult;
import com.tpy.product_warehouse.api.ResponseResult;
import com.tpy.product_warehouse.service.InOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class InOrderServiceImpl implements InOrderService {

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;
    @Autowired
    TransactionDefinition transactionDefinition;

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
    public ResponseResult findInOrderDetailByOrderNo(String orderNo) {
        List<Map> list = null;
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

        List list = null;
        switch (paramStype){
            case ORDERNO_STYPE:
                log.info("根据订单查询入仓单");
                try {
                    list = inOrderDao.findInOrderDetailByOrderNo(param);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            case CARDNO_STYPE:
                try {
                    list = inOrderDao.findWarehouseReceiptByCardNo(param);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                log.info("根据缸号查询入仓单");
                break;
            case BARCODE_STYPE:
                try {
                    list = inOrderDao.findWarehouseReceiptByBarCode(param);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                log.info("根据布号（条码）查询入仓单");
                break;
        }

        if(list == null){
            return CommonsResult.getFialResult("系统异常，查询失败");
        }
        return CommonsResult.getSuccessResult("查询成功",list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult addInStore(Map map) throws SQLException {
        // 1. 调用存储过程，创建入库单号
        String inStoreNo = inOrderDao.getInStoreNo();
        log.info("新增入库单号："+inStoreNo);
        map.put("inStoreNo",inStoreNo);
        // 2. 新增入库单号
        int i = inOrderDao.insertInStore(map);
        if(i == 0){
            return CommonsResult.getFialResult("入仓失败");
        }



        // 更新入库单数据
        int isSucces = inOrderDao.modifyInStore(inStoreNo);
        if(isSucces == 0){
            return CommonsResult.getFialResult("更新");
        }


        return CommonsResult.getSuccessResult("入仓成功");
    }
}
