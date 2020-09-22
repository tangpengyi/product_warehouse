package com.tpy.product_warehouse.service.impl;

import com.tpy.product_warehouse.api.CommonsResult;
import com.tpy.product_warehouse.api.ResponseResult;
import com.tpy.product_warehouse.dao.OutOrderDao;
import com.tpy.product_warehouse.service.OutOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
@Slf4j
public class OutOrderServiceImpl implements OutOrderService {

    @Autowired
    private OutOrderDao outOrderDao;

    @Override
    public ResponseResult getAllOutStore() {
        List outStore = null;
        try {
            outStore = outOrderDao.getOutStore();
        } catch (SQLException e) {
            log.error(e.getMessage());
            return CommonsResult.getFialResult("系统异常");
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            return CommonsResult.getFialResult("系统异常");
        }
        return CommonsResult.getSuccessResult("查询成功",outStore);
    }

    @Override
    public ResponseResult getOutStoreInfo(String uGUID) {
        List list = null;
        try {
            list = outOrderDao.getOutStoreInfoByGUID(uGUID);
        } catch (SQLException e) {
            log.error(e.getMessage());
            return CommonsResult.getFialResult("系统异常");
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            return CommonsResult.getFialResult("系统异常");
        }
        return CommonsResult.getSuccessResult("查询成功",list);
    }

    @Override
    public ResponseResult getOutStoreByBarCode(String uGUID) {
        List outStoreByBarCode = null;
        try {
            outStoreByBarCode = outOrderDao.getOutStoreByBarCode(uGUID);
        } catch (SQLException e) {
            log.error(e.getMessage());
            return CommonsResult.getFialResult("系统异常");
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            return CommonsResult.getFialResult("系统异常");
        }

        return CommonsResult.getSuccessResult("查询成功",outStoreByBarCode);
    }
}
