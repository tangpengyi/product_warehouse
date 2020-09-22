package com.tpy.product_warehouse.dao;

import com.tpy.product_warehouse.utils.JDBCUtils;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OutOrderDao {

    //根据GUID查询条码信息
    public List getOutStoreByBarCode(String uGUID) throws SQLException, ClassNotFoundException {

        ResultSet rs = JDBCUtils.queryData(getOutStoreByBarCodeSql(),uGUID);
        List list = new ArrayList();
        while(rs.next()){
            Map map = new HashMap();
            map.put("sBarCode",rs.getString("sBarCode"));
            map.put("nQty",rs.getBigDecimal("nQty"));
//            map.put("nLength",rs.getString("nLength"));
//            map.put("sRemark",rs.getString("sRemark"));
            map.put("tCreateTime",rs.getString("tCreateTime"));
            map.put("outStoreStype",rs.getString("outStoreStype"));
            list.add(map);
        }

        return list;
    }

    private String getOutStoreByBarCodeSql(){
        return "SELECT A.sBarCode,A.sSeq,B.nQty,B.nQty,CONVERT(DECIMAL(18,2), B.nQty*2.2047),\n" +
                "A.nQty,A.nQty,CONVERT(DECIMAL(18,2), A.nQty*2.2047),A.nLength,\n" +
                "A.nPkgExQty ,a.nNetQty,A.sRemark, A.tCreateTime,\n" +
                "CASE WHEN A.sCreator='AUTO' THEN '扫描出库'\n" +
                "ELSE '手动出库' END as 'outStoreStype'\n" +
                "FROM dbo.mmBarCodeOut A  WITH(NOLOCK)\n" +
                "INNER JOIN  dbo.mmBarCodeIn B WITH(NOLOCK) ON B.sBarCode=A.sBarCode\n" +
                "INNER JOIN dbo.mmSTInHdr C WITH(NOLOCK) ON C.uGUID=B.ummInHdrGUID AND C.ummStoreGUID='178E363C-BEEF-4D26-B571-9F0000FE919E'\n" +
                "WHERE a.ummOutDtlGUID=?";
    }

    //根据uGUID查询订单信息
    public List getOutStoreInfoByGUID(String uGUID) throws SQLException, ClassNotFoundException {

        ResultSet resultSet = JDBCUtils.queryData(getOutStoreInfoByGUIDSql(), uGUID);
        List list = new ArrayList();
        while(resultSet.next()){
            Map map = new HashMap();
            map.put("sCustomerOrderNo",resultSet.getString("sCustomerOrderNo"));
            map.put("uGUID",resultSet.getString("uGUID"));
            map.put("nStoreOutPkgQty",resultSet.getString("nStoreOutPkgQty"));
            map.put("nStoreOutNetQty",resultSet.getString("nStoreOutNetQty"));
            map.put("nStoreOutQty",resultSet.getString("nStoreOutQty"));
            map.put("sOrderNo",resultSet.getString("sOrderNo"));
            list.add(map);
        }
        return list;
    }

    private String getOutStoreInfoByGUIDSql(){
        return "Select * INTO # \n" +
                "FROM dbo.vwmmSTOutStoreDtl With(NoLock) \n" +
                "Where ummOutHdrGUID = ? \n" +
                "\n" +
                "SELECT uGUID,sCustomerOrderNo,nStoreOutPkgQty,nStoreOutNetQty,nStoreOutQty,sOrderNo FROM # ORDER BY uGUID \n" +
                "DROP TABLE #";
    }

    // 查询所有的出库订单
    public List getOutStore() throws SQLException, ClassNotFoundException {

        ResultSet rs = JDBCUtils.queryData(getOutStoreSql(), null);
        List list = new ArrayList();
        while(rs.next()){
            Map map = new HashMap();
            map.put("uGUID",rs.getString("uGUID"));
            map.put("sStoreOutNo",rs.getString("sStoreOutNo"));
            map.put("time",rs.getString("time"));
            map.put("sDestination",rs.getString("sDestination"));
            map.put("nOutPkgQtyEx",rs.getBigDecimal("nOutPkgQtyEx"));
            map.put("sCheckItemName",rs.getString("sCheckItemName"));
            map.put("nOutQtyEx",rs.getBigDecimal("nOutQtyEx"));
            list.add(map);
        }

        return list;
    }

    private String getOutStoreSql(){
        return "SELECT  * INTO #tmpStoreOutList \n" +
                "FROM (\n" +
                "\tSELECT A.* ,M.sStoreNo,M.sStoreName ,B.sCheckItemNo,B.sCheckItemName,B.sCheckItemFullName  \n" +
                "\tFROM dbo.mmSTOutHdr A WITH(NoLock)  \n" +
                "\tLEFT JOIN mmStore M WITH(NOLOCK) ON M.uGUID = A.ummStoreGUID  \n" +
                "\tLEFT JOIN vwmmSTStoreCheckItem B WITH(NOLOCK) ON B.uGUID = A.uRefDestGUID) A  \n" +
                "\tWHERE (iStoreOutStatus = 0) AND immStoreOutTypeID = 20  \n" +
                "\tAND ummStoreGUID in ('{178E363C-BEEF-4D26-B571-9F0000FE919E}') \n" +
                "\tORDER By tStoreOutTime DESC \n" +
                "\n" +
                "SELECT uGUID,sStoreOutNo,CONVERT(VARCHAR(20),tStoreOutTime,120) AS [time],sDestination,nOutPkgQtyEx,sCheckItemName,nOutQtyEx " +
                "FROM #tmpStoreOutList \n" +
                "DROP TABLE #tmpStoreOutList";
    }

}
