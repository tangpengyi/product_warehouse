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
    public ResponseResult getStoreInOrderByParam(String param,int paramStype) throws SQLException, ClassNotFoundException {
        List<Map> storeInOrders = null;
        switch (paramStype){
            case ORDERNO_STYPE:
                storeInOrders = inOrderDao.getStoreInOrderByOrderNo(param);
                break;
            case CARDNO_STYPE:
                storeInOrders = inOrderDao.getOrderByCardNo(param);
                break;
            case BARCODE_STYPE:
                storeInOrders = inOrderDao.getOrderNoByBarCode(param);
                break;
        }
        JDBCUtils.close();
        return CommonsResult.getSuccessResult("查询成功",storeInOrders);
    }

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
    public ResponseResult findInOrderDetailByOrderNo(List<Object> orderNos) throws SQLException {
        List<Map> result = new ArrayList<>();
        try {
            for(Object orderNo : orderNos){
                List<Map> list = inOrderDao.findInOrderDetailByOrderNo(orderNo.toString());
                result.addAll(list);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        JDBCUtils.close();
        if(result.size() != 0){
            return CommonsResult.getSuccessResult("查询成功",result);
        }
        return CommonsResult.getFialResult("系统异常");
    }

    @Override
    public ResponseResult findPendingOrderByNo(int paramStype, String param) throws SQLException {

        List list = null;
        switch (paramStype){
            case ORDERNO_STYPE:
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
    public ResponseResult addInStore(Map map) throws SQLException, ClassNotFoundException {

        Connection conn = null;

        try {
            conn = JDBCUtils.getConn();
            //开启事务
            conn.setAutoCommit(false);

            // 1. 调用存储过程，创建入库单号
            map.put("inStoreNo",inOrderDao.getInStoreNo());
        } catch (Exception e) {
            log.info(e.getMessage());
            return CommonsResult.getFialResult("入仓失败");
        }

        // 2. 新增入库单号 参数inStoreNo userName
        int i = 0;
        try {
            i = inOrderDao.insertInStore(map);
            //ummInHdrGUID 就是入库单id
            map.put("ummInHdrGUID",inOrderDao.getStoreInGUIDByStoreInNo((String) map.get("inStoreNo")));
            inOrderDao.modifyInStore((String) map.get("ummInHdrGUID"));
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

        List barCodes = (List<String>) map.get("barCode");
        //跟过条码查询对应的坯布数量
        Map barCodeParams = inOrderDao.getNInNetQtyByBarCodes(barCodes);

        Map params= null;
        try {
            params = inOrderDao.getInStoreInfo(map);
            params.put("nInQty",barCodeParams.get("nInNetQty"));
            params.put("nInNetQty",barCodeParams.get("nNetQty"));
            params.put("nInPkgExQty",barCodeParams.get("nPkgExQty"));
            //获取一个GUID
            String ummInDtlGUID = inOrderDao.getGUID();
            map.put("ummInDtlGUID",ummInDtlGUID);


            // 3. 根据订单获取入仓信息，为入仓提供参数 新增入仓
            params.put("ummInDtlGUID",ummInDtlGUID);
            inOrderDao.insertStoreInDtl(params);

            // 4. 调用存储过程审核订单，更新数据等信息
            inOrderDao.inStoreProcedure(map,0,1);

            // 5.获取条码信息 插入条码信息
            for(Object barCode : barCodes){
                Map fabricIfnoMap = inOrderDao.findFabricIfnoByBarcode(barCode.toString());
                if(fabricIfnoMap == null){
                    throw new Exception("查询布号信息为null");
                }
                fabricIfnoMap.put("sCreator",map.get("userName"));
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

            // 调用存储过程
            inOrderDao.inStoreProcedure(map,1,0);
            // 更新入库单数据
            inOrderDao.modifyInStore((String) map.get("ummInHdrGUID"));

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
