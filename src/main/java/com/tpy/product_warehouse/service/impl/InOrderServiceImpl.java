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
    public ResponseResult getStoreInOrderByParam(String param, int paramStype) throws SQLException, ClassNotFoundException {
        List<Map> storeInOrders = null;
        switch (paramStype) {
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
        return CommonsResult.getSuccessResult("查询成功", storeInOrders);
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
        if (pendingOrder != null) {
            return CommonsResult.getSuccessResult("查询成功", pendingOrder);
        }
        return CommonsResult.getFialResult("系统异常");
    }


    @Override
    public ResponseResult findInOrderDetailByOrderNo(List<Object> orderNos) throws SQLException {
        List<Map> result = new ArrayList<>();
        try {
            for (Object orderNo : orderNos) {
                List<Map> list = inOrderDao.findInOrderDetailByOrderNo(orderNo.toString());
                result.addAll(list);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        JDBCUtils.close();
        if (result.size() != 0) {
            return CommonsResult.getSuccessResult("查询成功", result);
        }
        return CommonsResult.getFialResult("系统异常");
    }

    @Override
    public ResponseResult findPendingOrderByNo(int paramStype, String param) throws SQLException {

        List list = null;
        switch (paramStype) {
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
        if (list == null) {
            return CommonsResult.getFialResult("系统异常，查询失败");
        }
        return CommonsResult.getSuccessResult("查询成功", list);
    }

    @Override
    public ResponseResult addInStore(List<Map<String, Object>> list) {

        Connection conn = null;
        String inStoreNo = null;
        String ummInHdrGUID = null;
        try {
            conn = JDBCUtils.getConn();
            conn.setAutoCommit(false);
            // 1. 调用存储过程，创建入库单号
            inStoreNo = inOrderDao.getInStoreNo();
        } catch (ClassNotFoundException e) {
            return rollBack(conn,e);
        } catch (SQLException e) {
            return rollBack(conn,e);
        }

        for (Map map : list) {
            map.put("inStoreNo", inStoreNo);
            // 2. 新增入库单号 参数inStoreNo userName
            int i = 0;
            //第一个就需要创建一个入库单
            try {

                if (list.get(0) == map) {
                    ummInHdrGUID = insertInStore(map);
                }
                map.put("ummInHdrGUID", ummInHdrGUID);

                List barCodes = (List<String>) map.get("barCode");
                //根据条码查询对应的条码信息
                Map barCodeParams = inOrderDao.getNInNetQtyByBarCodes(barCodes);

                // 3. 根据订单获取入仓信息，为入仓提供参数 新增入仓
                Map params = getInStoreInfo(map, barCodeParams);
                inOrderDao.insertStoreInDtl(params);

                map.put("ummInDtlGUID", params.get("ummInDtlGUID"));
                // 4. 调用存储过程审核订单，更新数据等信息
                inOrderDao.inStoreProcedure(map, 0, 1);
                // 5.获取条码信息 插入条码信息
                for (Object barCode : barCodes) {
                    //获取新增条码所需数据
                    Map fabricIfnoMap = getBarCodeInfo(barCode, map);

                    //条码入仓 ，入仓失败
                    if (inOrderDao.barCodeIn(fabricIfnoMap) == 0) {
                        new Exception("入仓失败");
                    }
                }

                // 调用存储过程
                inOrderDao.inStoreProcedure(map, 1, 0);
                // 更新入库单数据
                inOrderDao.modifyInStore((String) map.get("ummInHdrGUID"));
            } catch (SQLException e) {
                return rollBack(conn,e);
            } catch (ClassNotFoundException e) {
                return rollBack(conn,e);
            } catch (Exception e) {
                return rollBack(conn,e);
            }
        }

        //提交事务
        try {
            conn.commit();
            conn.setAutoCommit(true);
            JDBCUtils.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return CommonsResult.getSuccessResult("入仓成功");
    }

    public String insertInStore(Map map) throws SQLException, ClassNotFoundException {
        int isSuccess = inOrderDao.insertInStore(map);
        //ummInHdrGUID 就是入库单id
        String ummInHdrGUID = inOrderDao.getStoreInGUIDByStoreInNo((String) map.get("inStoreNo"));
        inOrderDao.modifyInStore(ummInHdrGUID);

        if (isSuccess == 0) {
            new Exception("添加入仓订单失败");
        }
        return ummInHdrGUID;
    }

    public Map getInStoreInfo(Map map, Map barCodeParams) throws SQLException, ClassNotFoundException {
        Map params = inOrderDao.getInStoreInfo(map);
        params.put("nInQty", barCodeParams.get("nInNetQty"));
        params.put("nInNetQty", barCodeParams.get("nNetQty"));
        params.put("nInPkgExQty", barCodeParams.get("nPkgExQty"));
        //获取一个GUID
        params.put("ummInDtlGUID", inOrderDao.getGUID());
        return params;
    }

    public Map getBarCodeInfo(Object barCode, Map map) throws Exception {
        Map fabricIfnoMap = inOrderDao.findFabricIfnoByBarcode(barCode.toString());
        if (fabricIfnoMap == null) {
            throw new Exception("查询布号信息为null");
        }
        fabricIfnoMap.put("sCreator", map.get("userName"));
        fabricIfnoMap.put("sBarCode", barCode);
        fabricIfnoMap.put("ummInHdrGUID", map.get("ummInHdrGUID"));
        fabricIfnoMap.put("ummInDtlGUID", map.get("ummInDtlGUID"));
        return fabricIfnoMap;
    }

    public ResponseResult rollBack(Connection conn,Exception e){
        try {
            conn.rollback();
            JDBCUtils.close();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        return CommonsResult.getFialResult(e.getMessage());
    }

}
