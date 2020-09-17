package com.tpy.product_warehouse.service.impl;

import com.tpy.product_warehouse.dao.InOrderDao;
import com.tpy.product_warehouse.api.CommonsResult;
import com.tpy.product_warehouse.api.ResponseResult;
import com.tpy.product_warehouse.service.InOrderService;
import com.tpy.product_warehouse.utils.JDBCUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class InOrderServiceImpl implements InOrderService {

    private final static int ORDERNO_STYPE = 1;
    private final static int CARDNO_STYPE = 2;
    private final static int BARCODE_STYPE = 3;

    @Autowired
    private InOrderDao inOrderDao;

    @Override
    public ResponseResult findAllPendingOrder() throws SQLException {
        ArrayList<Map> pendingOrder = null;
        try {
            pendingOrder = inOrderDao.findAllPendingOrder();
        } catch (SQLException e) {
            log.error("查询带入仓订单出错");
        } catch (ClassNotFoundException e) {
            log.error("查询带入仓订单出错");
        }
        JDBCUtils.close();
        if(pendingOrder != null){
            return CommonsResult.getSuccessResult("查询成功",pendingOrder);
        }
        return CommonsResult.getFialResult("系统异常");
    }

    @Override
    public ResponseResult findInOrderDetailByOrderNo(String orderNo) throws SQLException {
        List<Map> list = null;
        try {
            list = inOrderDao.findInOrderDetailByOrderNo(orderNo);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        JDBCUtils.close();
        if(list != null){
            return CommonsResult.getSuccessResult("查询成功",list);
        }
        return CommonsResult.getFialResult("系统异常");
    }

    @Override
    public ResponseResult findPendingOrderByNo(int paramStype, String param) throws SQLException {

        List list = null;
        switch (paramStype){
            case ORDERNO_STYPE:
                log.info("根据订单查询入仓单");
                try {
                    list = inOrderDao.findInOrderDetailByOrderNo(param);
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case CARDNO_STYPE:
                try {
                    list = inOrderDao.findWarehouseReceiptByCardNo(param);
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                log.info("根据缸号查询入仓单");
                break;
            case BARCODE_STYPE:
                try {
                    list = inOrderDao.findWarehouseReceiptByBarCode(param);
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                log.info("根据布号（条码）查询入仓单");
                break;
        }
        JDBCUtils.close();
        if(list == null){
            return CommonsResult.getFialResult("系统异常，查询失败");
        }
        return CommonsResult.getSuccessResult("查询成功",list);
    }

    @Override
    public ResponseResult addInStore(Map map){

        Connection conn = null;
        String inStoreNo = null;

        try {
            conn = JDBCUtils.getConn();
            // 开启事务

            conn.setAutoCommit(false);
            // 1. 调用存储过程，创建入库单号
            inStoreNo = inOrderDao.getInStoreNo();
            map.put("inStoreNo",inStoreNo);
        } catch (Exception e) {
            log.info(e.getMessage());
            return CommonsResult.getFialResult("入仓失败");
        }

        // 2. 新增入库单号 参数inStoreNo loginName
        int i = 0;
        try {
            i = inOrderDao.insertInStore(map);
            //ummInHdrGUID 就是入库单id
            map.put("ummInHdrGUID",inOrderDao.getStoreInGUIDByStoreInNo(inStoreNo));
        } catch (Exception e) {
            try {
                conn.rollback();
                JDBCUtils.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        if(i == 0){
            try {
                conn.rollback();
                JDBCUtils.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return CommonsResult.getFialResult("入仓失败");
        }
        map.put("location","A");
        map.put("shelfNo","a03");
        Map map1= null;
        try {
            map1 = inOrderDao.getInStoreInfo(map);
            // 3. 根据订单获取入仓信息，为入仓提供参数 新增入仓
            String ummInDtlGUID = inOrderDao.insertStoreInDtl(map1);
            if(StringUtils.isEmpty(ummInDtlGUID)){
                conn.rollback();
                JDBCUtils.close();
                return CommonsResult.getFialResult("入仓失败");
            }

            List barCodes = (List<String>) map.get("barCode");
            // 4.获取条码信息 插入条码信息
            for(Object barCode : barCodes){
                Map fabricIfnoMap = inOrderDao.findFabricIfnoByBarcode(barCode.toString());
                if(fabricIfnoMap == null){
                    throw new Exception("查询布号信息为null");
                }
                fabricIfnoMap.put("sCreator",map.get("loginName"));
                fabricIfnoMap.put("sBarCode",barCode);
                fabricIfnoMap.put("ummInHdrGUID",map.get("ummInHdrGUID"));
                fabricIfnoMap.put("ummInDtlGUID",ummInDtlGUID);
                //条码入仓 ，入仓失败
                if(inOrderDao.barCodeIn(fabricIfnoMap) == 0){
                    conn.rollback();
                    JDBCUtils.close();
                    return CommonsResult.getFialResult("入仓失败");
                }
            }

            // 更新入库单数据
            inOrderDao.modifyInStore((String) map.get("ummInHdrGUID"));

            String ummInHdrGUID = (String) map1.get("ummInHdrGUID");
            //提交事务
            conn.commit();
            conn.setAutoCommit(true);
            JDBCUtils.close();

        } catch (Exception e) {
            try {
                conn.rollback();
                JDBCUtils.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            log.error(e.getMessage());
            return CommonsResult.getFialResult("入仓失败");
        }


        return CommonsResult.getSuccessResult("入仓成功");
    }
}
