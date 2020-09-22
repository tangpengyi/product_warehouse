package com.tpy.product_warehouse.dao;

import com.tpy.product_warehouse.utils.JDBCUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class InOrderDao {

    // 根据条码号查询入仓信息
    public List<Map> getOrderNoByBarCode(String barCode) throws SQLException, ClassNotFoundException {
        ResultSet resultSet = JDBCUtils.queryData(getOrderNoByBarCodeSql(), barCode);
        String sStoreInNo = null;
        while(resultSet.next()){
            sStoreInNo = resultSet.getString("sStoreInNo");
        }
        return getStoreInOrderByOrderNo(sStoreInNo);
    }


    //根据缸号查询订单
    public List<Map> getOrderByCardNo(String cardNo) throws SQLException, ClassNotFoundException {
        ResultSet resultSet = JDBCUtils.queryData(getOrderNoByCardNoSql(), cardNo);
        List<Map> list = new ArrayList<>();
        while(resultSet.next()){
            String orderNo = resultSet.getString("store_in_no");
            list.addAll(getStoreInOrderByOrderNo(orderNo));
        }
        return list;
    }

    //根据缸号查询订单
    public String getOrderNoByCardNoSql(){
        return "select h.[sStoreInNo] as [store_in_no]\n" +
                "\tfrom [HSWeaveDyeingERP_TEST].[dbo].[mmSTInHdr] h with(nolock) \n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmSTInDtl] d with(nolock) on h.[uGUID] = d.[ummInHdrGUID]\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmBarCodeIn] bi with(nolock) on d.uGUID = bi.[ummInDtlGUID]\t\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmStore] s with(nolock) on h.[ummStoreGUID] = s.[uGUID]\t\n" +
                "\tWHERE d.[sCardNo] = ? and s.[sStoreName] = '质检中转仓'\n" +
                "\tGROUP BY h.[sStoreInNo]";
    }

    //根据订单号查询订单信息
    public List<Map> getStoreInOrderByOrderNo(String OrderNo) throws SQLException, ClassNotFoundException {

        ResultSet resultSet = JDBCUtils.queryData(getWeightByOrderNoSql(), OrderNo);
        String weight = null;
        while(resultSet.next()){
            weight = resultSet.getString("weight");
        }

        ResultSet rs = JDBCUtils.queryData(getStoreInOrderSql(), OrderNo);
        List list = new ArrayList();
        while(rs.next()){{
            Map map = new HashMap();
            map.put("order_no",rs.getString("store_in_no"));
            map.put("time",rs.getString("store_in_time"));
            map.put("material_no",rs.getString("material_no"));
            map.put("quantity",rs.getString("quantity"));
            map.put("weight",weight);
            map.put("selected",false);
            list.add(map);
        }}

        return list;
    }

    private String getWeightByOrderNoSql(){
        return "declare @order_no nvarchar(50) = ?\n" +
                "\n" +
                "select SUM(bi.[nQty]) AS [weight]\n" +
                "\tfrom [HSWeaveDyeingERP_TEST].[dbo].[Split](@order_no, ',') o\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmSTInHdr] h with(nolock) on o.[Value] = h.[sStoreInNo]\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmSTInDtl] d with(nolock) on h.[uGUID] = d.[ummInHdrGUID]\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmBarCodeIn] bi with(nolock) on d.uGUID = bi.[ummInDtlGUID]\t\n" +
                "\tGROUP BY h.[sStoreInNo]";
    }

    private String getStoreInOrderSql(){
        return "\n" +
                "-- 获取质检中转区待入仓的订单号码\n" +
                "set rowcount 0\n" +
                "select d.[uGUID]\t\n" +
                "\t, h.[sStoreInNo]\n" +
                "\t, h.[tStoreInTime]\n" +
                "\t, datediff(minute, h.tStoreInTime, h.[tPrintTime]) diff_minutes\n" +
                "\t, h.[tPrintTime]\t\n" +
                "\t, d.[sMaterialNo]\n" +
                "\t, d.[sCardNo]\n" +
                "\t, d.[nInPkgQty]\n" +
                "\t, d.[nInQty]\t\n" +
                "\t, d.[nStockPkgQty]\n" +
                "\tinto #t1\n" +
                "\tfrom [HSWeaveDyeingERP_TEST].[dbo].[mmSTInHdr] h with(nolock)\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmStore] s with(nolock) on h.[ummStoreGUID] = s.[uGUID]\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmSTInDtl] d with(nolock) on h.[uGUID] = d.[ummInHdrGUID]\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmStoreInType] t with(nolock) on h.[immStoreInTypeID] = t.[iID]\n" +
                "\tWHERE h.[tStoreInTime] > dateadd(month, -1, getdate())\n" +
                "\t\tand s.[sStoreName] = '质检中转仓'\n" +
                "\t\tand d.[sCardNo] not like 'B[FR]%'\t\t--BF,RF开头的缸号，不允许入正品仓\n" +
                "\t\tand [h].[iStoreInStatus] = 1\n" +
                "\t\tAND h.[sStoreInNo] = ?  --订单号\n" +
                "\n" +
                "-- 取出已经入库的数量\n" +
                "select t.[uGUID]\n" +
                "\t, t.[sCardNo]\n" +
                "\t, sum(d.[nInPkgQty]) as nInPkgQty\n" +
                "\t, sum(d.[nInQty]) as nInQty\n" +
                "\tinto #t2\n" +
                "\tfrom [#t1] t\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmSTInDtl] d with(nolock) on t.uGUID = d.[uRefDtlGUID] and t.sCardNo = d.[sCardNo]\t\n" +
                "\tgroup by t.[uGUID], t.[sCardNo]\n" +
                "\n" +
                "-- 减去已经入库的数量\n" +
                "update t1\n" +
                "\tset t1.nInQty = t1.nInQty - t2.[nInQty]\n" +
                "\t, t1.nInPkgQty = t1.nInPkgQty - t2.[nInPkgQty]\n" +
                "\tfrom #t1 t1\n" +
                "\tjoin #t2 t2 on t1.[uGUID] = t2.[uGUID]\n" +
                "\n" +
                "delete from #t1 where [nInPkgQty] <= 0\n" +
                "\n" +
                "select [sStoreInNo] as [store_in_no]\n" +
                "\t, isnull(CONVERT(VARCHAR(20),[tprinttime],120), [tStoreInTime]) as [store_in_time]\n" +
                "\t, [sMaterialNo] as [material_no]\n" +
                "\t, [nInPkgQty] as [quantity]\n" +
                "\tfrom [#t1] \n" +
                "\torder by [store_in_time]\n" +
                "\n" +
                "\n" +
                "drop table [#t1], [#t2]\n" +
                "\n" +
                "\n";
    }

    public Map getNInNetQtyByBarCodes(List barCodes) throws SQLException, ClassNotFoundException {

        StringBuffer param = new StringBuffer();
        int count = 0;
        for(Object barCode : barCodes){
            count ++;
            param.append("?");
            if(barCodes.size() != count){
                param.append(",");
            }
        }
        //          入库数量    坯布数量    辅助数
        String sql = "SELECT SUM(E.nQty) AS nInNetQty\n" +
                "                ,sum(E.nNetQty) AS nNetQty \n" +
                "                ,sum(E.nPkgExQty) AS nPkgExQty\n" +
                " FROM dbo.mmSTInHdr AS C WITH(NOLOCK)\n" +
                "           INNER JOIN dbo.mmSTInDtl AS L WITH(NOLOCK) ON C.uGUID=L.ummInHdrGUID\n" +
                "                INNER JOIN dbo.mmBarCodeIn AS E  WITH(NOLOCK) ON L.uGUID=ummInDtlGUID\n" +
                "                LEFT JOIN dbo.mmStore HY WITH(NOLOCK) ON HY.uGUID=C.ummStoreGUID\n" +
                "                WHERE E.sBarCode in ("+param+")  AND HY.sStoreNo='G026'\n" +
                "                AND (E.sBarCode NOT IN (SELECT sBarCode FROM dbo.mmBarCodeIn C \n" +
                "                INNER JOIN dbo.mmSTInHdr D ON D.uGUID=C.ummInHdrGUID\n" +
                "                INNER JOIN dbo.mmStore E ON D.ummStoreGUID=E.uGUID \n" +
                "                WHERE E.sStoreNo IN ('G02','G025','G023'))\n" +
                "                OR ISNULL(E.sStatus,'')='入退')";

        ResultSet resultSet = JDBCUtils.querySqlByList(sql, barCodes);
        Map map = new HashMap();
        while(resultSet.next()){
            //入库数量
            map.put("nInNetQty",resultSet.getObject("nInNetQty"));
            map.put("nNetQty",resultSet.getObject("nNetQty"));
            map.put("nPkgExQty",resultSet.getObject("nPkgExQty"));
        }
        return map;
    }

    public int inStoreProcedure(Map map,int checkMinus,int cancel) throws SQLException, ClassNotFoundException {
        String sql = "EXEC dbo.spmmSTStoreBarCodeIn @ummInHdrGUID = ? ,\n" +
                "\t @ummInDtlGUIDList = ? , @sUserNo = ? ,\n" +
                "\t  @bCheckMinus = ? , @bCancel = ?";
        return JDBCUtils.executeSql(sql, map.get("ummInHdrGUID"),map.get("ummInDtlGUID"),map.get("userName"),checkMinus,cancel);
    }

    /**
     * 获取GUID
     * @return
     */
    public String getGUID() throws SQLException, ClassNotFoundException {
        String uGUID = null;
        ResultSet rs = JDBCUtils.queryData("select newId() as uGUID", null);
        while(rs.next()){
            uGUID = rs.getString("uGUID");
        }
        return uGUID;
    }

    //根据入库编号查询id
    public String getStoreInGUIDByStoreInNo(String orderNo) throws SQLException, ClassNotFoundException {
        ResultSet rs = JDBCUtils.queryData("SELECT uGUID FROM mmSTInHdr WHERE sStoreInNo = ?", orderNo);
        String uGUID = null;
        while(rs.next()){
            uGUID = rs.getString("uGUID");
        }
        return uGUID;
    }

    //条码入仓
    public int barCodeIn(Map map) throws SQLException, ClassNotFoundException {
        List list = new ArrayList();
        list.add(map.get("ummInHdrGUID"));
        list.add(map.get("ummInDtlGUID"));
        list.add(map.get("sSeq"));
        list.add(map.get("sBarCode"));
        list.add(map.get("nQty"));
        list.add(map.get("nNetQty"));
        list.add(map.get("nGrossQty"));
        list.add(map.get("nPkgExQty"));
        list.add(map.get("nLength"));
        list.add(map.get("sCreator"));
        return JDBCUtils.executeSqlByList(barCodeInSql(),list);
    }

    // 条码入库sql
    private String barCodeInSql(){
        return "INSERT INTO dbo.mmBarCodeIn \n" +
                "(uGUID,ummInHdrGUID,ummInDtlGUID,sSeq,sBarCode,nQty,nNetQty,nGrossQty,nPkgExQty\n" +
                ",nLength,sCreator,tCreateTime) \n" +
                "VALUES(NEWID(),?,?,?,?,?,?,?,?,?,?,CONVERT(VARCHAR(20),GETDATE(),120))";
    }

    /**
     * 根据条码获取布信息
     * @param barCode
     * @return
     */
    public Map findFabricIfnoByBarcode(String barCode) throws SQLException, ClassNotFoundException {
        ResultSet rs = JDBCUtils.queryData(findFabricIfnoByBarcodeSql(), barCode);
        Map map = null;
        while(rs.next()){
            map = new HashMap();
            map.put("ummInHdrGUID",rs.getString("ummInHdrGUID"));
            map.put("ummInDtlGUID",rs.getString("ummInDtlGUID"));
            map.put("sSeq",rs.getString("sSeq"));
            map.put("nQty",rs.getBigDecimal("nQty"));
            map.put("nNetQty",rs.getBigDecimal("nNetQty"));
            map.put("nGrossQty",rs.getBigDecimal("nGrossQty"));
            map.put("nPkgExQty",rs.getBigDecimal("nPkgExQty"));
            map.put("nLength",rs.getBigDecimal("nLength"));
        }
        return map;
    }

    private String findFabricIfnoByBarcodeSql(){
        return "SELECT \n" +
                "C.uGUID AS ummInHdrGUID,L.uGUID AS ummInDtlGUID,E.uGUID,E.sSeq,E.sBarCode\n" +
                ",nQty=E.nQty, E.nLength, nNetQty=E.nNetQty\n" +
                ",nGrossQty=E.nQty*2.2044,E.nPkgExQty\n" +
                "FROM dbo.mmSTInHdr AS C WITH(NOLOCK)\n" +
                "INNER JOIN dbo.mmSTInDtl AS L WITH(NOLOCK) ON C.uGUID=L.ummInHdrGUID\n" +
                "INNER JOIN dbo.mmBarCodeIn AS E  WITH(NOLOCK) ON L.uGUID=ummInDtlGUID\n" +
                "LEFT JOIN dbo.mmStore HY WITH(NOLOCK) ON HY.uGUID=C.ummStoreGUID\n" +
                "WHERE E.sBarCode = ?  AND HY.sStoreNo='G026'\n" +
                "AND (E.sBarCode NOT IN (SELECT sBarCode FROM dbo.mmBarCodeIn C \n" +
                "INNER JOIN dbo.mmSTInHdr D ON D.uGUID=C.ummInHdrGUID\n" +
                "INNER JOIN dbo.mmStore E ON D.ummStoreGUID=E.uGUID \n" +
                "WHERE E.sStoreNo IN ('G02','G025','G023'))\n" +
                "OR ISNULL(E.sStatus,'')='入退')";
    }

    /**
     * 新增入库信息
     * @param
     * @return
     */
    public int insertStoreInDtl(List list) throws SQLException, ClassNotFoundException {
        return JDBCUtils.executeSqlByList(insertStoreInDtlSql(), list);
    }

    public int insertStoreInDtl(Map map) throws SQLException, ClassNotFoundException {
        List list = new ArrayList<Object>();
        list.add(map.get("ummInDtlGUID"));
        list.add(map.get("ummInHdrGUID"));
        list.add(map.get("ummMaterialGUID"));
        list.add(map.get("utmColorGUID"));
        list.add(map.get("usdOrderLotGUID"));
        list.add(map.get("uRefDtlGUID"));
        list.add(map.get("upbCustomerGUID"));

        return JDBCUtils.executeSqlByList(insertStoreInDtlSql2(map), list);
    }

    private String insertStoreInDtlSql2(Map map){
        if(map.get("sCustomerMaterialNo") == null){
            map.put("sCustomerMaterialNo","");
        }
        String sql = "DECLARE @uGUID UNIQUEIDENTIFIER=?,@ummInHdrGUID UNIQUEIDENTIFIER=?,@ummMaterialGUID UNIQUEIDENTIFIER=?,@utmColorGUID UNIQUEIDENTIFIER=?\n" +
                ",@usdOrderLotGUID UNIQUEIDENTIFIER=?,@uRefDtlGUID UNIQUEIDENTIFIER=?,@sMaterialNo nvarchar(100)='"+map.get("sMaterialNo")+"'\n" +
                ",@sMaterialName nvarchar(200)='"+map.get("sMaterialName")+"',@sComponent nvarchar(400)='"+map.get("sComponent")+"',@sColorNo nvarchar(100)='"+map.get("sColorNo")+"',@sColorName nvarchar(200)='"+map.get("sColorName")+"'\n" +
                ",@sMaterialLot NVARCHAR(100)='"+map.get("sMaterialLot")+"',@sCardNo nvarchar(50)='"+map.get("sCardNo")+"',@sGrade nvarchar(20)='"+map.get("sGrade")+"',@sLocation nvarchar(50)='"+map.get("sLocation")+"'\n" +
                ",@nInQty decimal(18, 3)="+map.get("nInQty")+",@nInGrossQty decimal(18, 3)="+map.get("nInGrossQty")+",@sUnit nvarchar(10)='"+map.get("sUnit")+"',@nInPkgQty decimal(18, 3)="+map.get("nInPkgQty")+"\n" +
                ",@nInPkgUnitQty decimal(18, 3)="+map.get("nInPkgUnitQty")+",@nInPkgExQty decimal(18, 3)="+map.get("nInPkgExQty")+",@nIniPrice decimal(18, 6)="+map.get("nIniPrice")+",@sOrderNo nvarchar(100)='"+map.get("sOrderNo")+"'\n" +
                ",@sOrderColorNo nvarchar(100)='"+map.get("sOrderColorNo")+"',@sCustomerOrderNo nvarchar(100)='"+map.get("sCustomerOrderNo")+"',@sCustomerMaterialNo nvarchar(100)='"+map.get("sCustomerMaterialNo")+"'\n" +
                ",@sRemark nvarchar(1000)='"+map.get("sRemark")+"',@nDiff04 decimal(9, 4)="+map.get("nDiff04")+",@sDiff29 nvarchar(100)='"+map.get("sDiff29")+"',@sDiff01 nvarchar(100)='"+map.get("sDiff01")+"'\n" +
                ",@upbCustomerGUID UNIQUEIDENTIFIER=?,@sUsage nvarchar(10)='"+map.get("sUsage")+"',@sYarnInfo nvarchar(500)='"+map.get("sYarnInfo")+"',@nYarnLength decimal(18, 2)='"+map.get("nYarnLength")+"'\n" +
                ",@sMaterialTypeName nvarchar(50)='"+map.get("sMaterialTypeName")+"',@sPatternNo nvarchar(50)='"+map.get("sPatternNo")+"',@sDtlQtyList nvarchar(4000)='"+map.get("sDtlQtyList")+"',@sProductWidth nvarchar(20)='"+map.get("sProductWidth")+"'\n" +
                ",@sProductGMWT nvarchar(20)='"+map.get("sProductGMWT")+"',@sFinishingMethod nvarchar(150)='"+map.get("sFinishingMethod")+"',@sShelfNo nvarchar(50)='"+map.get("sShelfNo")+"',@nInLength decimal(18, 3)="+map.get("nInLength")+"\n" +
                ",@nInWeight decimal(18, 3)="+map.get("nInWeight")+",@sWeightUnit nvarchar(10)='"+map.get("sWeightUnit")+"',@nInNetQty decimal(18, 2)="+map.get("nInNetQty")+",@sCustomerSpecification nvarchar(200)='"+map.get("nInNetQty")+"'\n" +
                "\n" +
                "\n" +
                "INSERT INTO dbo.mmSTInDtl \n" +
                "(\n" +
                "uGUID,ummInHdrGUID,ummMaterialGUID,utmColorGUID,usdOrderLotGUID,uRefDtlGUID\n" +
                ",sMaterialNo,sMaterialName,sComponent,sColorNo,sColorName,sMaterialLot\n" +
                ",sCardNo,sGrade,sLocation,nInQty, nInGrossQty, sUnit\n" +
                ",nInPkgQty,nInPkgUnitQty,nInPkgExQty,nIniPrice,  nACPrice,  nAmount\n" +
                ",nTaxRate,nTaxAmount,nNoTaxAmount,nExchangeRate,nStockQty,nStockGrossQty\n" +
                ",nStockPkgQty,nStockPkgExQty,nStockAmount,sOrderNo,sOrderColorNo,sCustomerOrderNo\n" +
                ",sCustomerMaterialNo,sRemark,sDiff01,sDiff29,nDiff01,nDiff02\n" +
                ",nDiff03,nDiff04,nDiff05,nDiff06,nDiff07,nDiff08\n" +
                ",nDiff09,nDiff10,nDiff11,nDiff12,nDiff13,nDiff14\n" +
                ",nDiff15,nDiff16,nDiff17,nDiff18,nDiff19,nDiff20\n" +
                ",nDiff21,nDiff22,nDiff23,nDiff24,nDiff25,nDiff26\n" +
                ",nDiff27,nDiff29,nDiff30,upbCustomerGUID,sUsage,sYarnInfo\n" +
                ",nNeedleQty,nDialDiameter,nYarnLength,sMaterialTypeName,sPatternNo,sDtlQtyList\n" +
                ",sProductWidth,sProductGMWT,sFinishingMethod,sShelfNo,nInRawQty,nInSmallQty\n" +
                "\n" +
                ",nInLength,nInWeight,sWeightUnit,nOSPrice,nOSAddAmount,nSalesPrice\n" +
                ",nSalesAddAmount,nAmountNat,nTaxAmountNat,nNoTaxAmountNat,nStockAmountNat,nStockRawQty\n" +
                ",nStockSmallQty,nStockLength,nStockWeight,nInCostPrice,nInCostAmount,nStockCostAmount\n" +
                ",sCustomerSpecification,nInNetQty,nStockNetQty)  --105\n" +
                "\n" +
                "VALUES(\n" +
                "@uGUID,@ummInHdrGUID,@ummMaterialGUID,@utmColorGUID,@usdOrderLotGUID,@uRefDtlGUID,\n" +
//                "?,?,?,?,?,\n" +
                "@sMaterialNo,@sMaterialName,@sComponent,@sColorNo,@sColorName,@sMaterialLot\n" +
                ",@sCardNo,@sGrade,@sLocation,@nInQty,@nInGrossQty,@sUnit\n" +
                ",@nInPkgQty,@nInPkgUnitQty,@nInPkgExQty,@nIniPrice,0,0,\n" +
                "0,0,0,0,0,0,\n" +
                "0,0,0,@sOrderNo,@sOrderColorNo,@sCustomerOrderNo\n" +
                ",@sCustomerMaterialNo,@sRemark,@sDiff01,@sDiff29,0,0\n" +
                ",0,@nDiff04,0,0,0,0\n" +
                ",0,0,0,0,0,0\n" +
                ",0,0,0,0,0,0\n" +
                ",0,0,0,0,0,0\n" +
                ",0,0,0,@upbCustomerGUID,@sUsage,@sYarnInfo\n" +
//                ",0,0,0,?,@sUsage,@sYarnInfo\n" +
                ",0,0,@nYarnLength,@sMaterialTypeName,@sPatternNo,@sDtlQtyList\n" +
                ",@sProductWidth,@sProductGMWT,@sFinishingMethod,@sShelfNo,0,0\n" +
                "\n" +
                ",@nInLength,@nInWeight,@sWeightUnit,0,0,0\n" +
                ",0,0,0,0,0,0\n" +
                ",0,0,0,0,0,0\n" +
                ",@sCustomerSpecification,@nInNetQty,0)";
        return sql;
    }

    // 入库信息sql
    private String insertStoreInDtlSql(){
        return "DECLARE @ummInHdrGUID UNIQUEIDENTIFIER=?,@ummMaterialGUID UNIQUEIDENTIFIER=?,@utmColorGUID UNIQUEIDENTIFIER=?\n" +
                ",@usdOrderLotGUID UNIQUEIDENTIFIER=?,@uRefDtlGUID UNIQUEIDENTIFIER=?,@sMaterialNo nvarchar(100)=?\n" +
                ",@sMaterialName nvarchar(200)=?,@sComponent nvarchar(400)=?,@sColorNo nvarchar(100)=?,@sColorName nvarchar(200)=?\n" +
                ",@sMaterialLot NVARCHAR(100)=?,@sCardNo nvarchar(50)=?,@sGrade nvarchar(20)=?,@sLocation nvarchar(50)=?\n" +
                ",@nInQty decimal(18, 3)=?,@nInGrossQty decimal(18, 3)=?,@sUnit nvarchar(10)=?,@nInPkgQty decimal(18, 3)=?\n" +
                ",@nInPkgUnitQty decimal(18, 3)=?,@nInPkgExQty decimal(18, 3)=?,@nIniPrice decimal(18, 6)=?,@sOrderNo nvarchar(100)=?\n" +
                ",@sOrderColorNo nvarchar(100)=?,@sCustomerOrderNo nvarchar(100)=?,@sCustomerMaterialNo nvarchar(100)=?\n" +
                ",@sRemark nvarchar(1000)=?,@nDiff04 decimal(9, 4)=?,@sDiff29 nvarchar(100)=?,@sDiff01 nvarchar(100)=?\n" +
                ",@upbCustomerGUID UNIQUEIDENTIFIER=?,@sUsage nvarchar(10)=?,@sYarnInfo nvarchar(500)=?,@nYarnLength decimal(18, 2)=?\n" +
                ",@sMaterialTypeName nvarchar(50)=?,@sPatternNo nvarchar(50)=?,@sDtlQtyList nvarchar(4000)=?,@sProductWidth nvarchar(20)=?\n" +
                ",@sProductGMWT nvarchar(20)=?,@sFinishingMethod nvarchar(150)=?,@sShelfNo nvarchar(50)=?,@nInLength decimal(18, 3)=?\n" +
                ",@nInWeight decimal(18, 3)=?,@sWeightUnit nvarchar(10)=?,@nInNetQty decimal(18, 2)=?,@sCustomerSpecification nvarchar(200)=?\n" +
                "\n" +
                "\n" +
                "INSERT INTO dbo.mmSTInDtl \n" +
                "(\n" +
                "ummInHdrGUID,ummMaterialGUID,utmColorGUID,usdOrderLotGUID,uRefDtlGUID\n" +
                ",sMaterialNo,sMaterialName,sComponent,sColorNo,sColorName,sMaterialLot\n" +
                ",sCardNo,sGrade,sLocation,nInQty, nInGrossQty, sUnit\n" +
                ",nInPkgQty,nInPkgUnitQty,nInPkgExQty,nIniPrice,  nACPrice,  nAmount\n" +
                ",nTaxRate,nTaxAmount,nNoTaxAmount,nExchangeRate,nStockQty,nStockGrossQty\n" +
                ",nStockPkgQty,nStockPkgExQty,nStockAmount,sOrderNo,sOrderColorNo,sCustomerOrderNo\n" +
                ",sCustomerMaterialNo,sRemark,sDiff01,sDiff29,nDiff01,nDiff02\n" +
                ",nDiff03,nDiff04,nDiff05,nDiff06,nDiff07,nDiff08\n" +
                ",nDiff09,nDiff10,nDiff11,nDiff12,nDiff13,nDiff14\n" +
                ",nDiff15,nDiff16,nDiff17,nDiff18,nDiff19,nDiff20\n" +
                ",nDiff21,nDiff22,nDiff23,nDiff24,nDiff25,nDiff26\n" +
                ",nDiff27,nDiff29,nDiff30,upbCustomerGUID,sUsage,sYarnInfo\n" +
                ",nNeedleQty,nDialDiameter,nYarnLength,sMaterialTypeName,sPatternNo,sDtlQtyList\n" +
                ",sProductWidth,sProductGMWT,sFinishingMethod,sShelfNo,nInRawQty,nInSmallQty\n" +
                "\n" +
                ",nInLength,nInWeight,sWeightUnit,nOSPrice,nOSAddAmount,nSalesPrice\n" +
                ",nSalesAddAmount,nAmountNat,nTaxAmountNat,nNoTaxAmountNat,nStockAmountNat,nStockRawQty\n" +
                ",nStockSmallQty,nStockLength,nStockWeight,nInCostPrice,nInCostAmount,nStockCostAmount\n" +
                ",sCustomerSpecification,nInNetQty,nStockNetQty)  --105\n" +
                "\n" +
                "VALUES(\n" +
                "@ummInHdrGUID,@ummMaterialGUID,@utmColorGUID,@usdOrderLotGUID,@uRefDtlGUID,\n" +
                "@sMaterialNo,@sMaterialName,@sComponent,@sColorNo,@sColorName,@sMaterialLot\n" +
                ",@sCardNo,@sGrade,@sLocation,@nInQty,@nInGrossQty,@sUnit\n" +
                ",@nInPkgQty,@nInPkgUnitQty,@nInPkgExQty,@nIniPrice,0,0,\n" +
                "0,0,0,0,0,0,\n" +
                "0,0,0,@sOrderNo,@sOrderColorNo,@sCustomerOrderNo\n" +
                ",@sCustomerMaterialNo,@sRemark,@sDiff01,@sDiff29,0,0\n" +
                ",0,@nDiff04,0,0,0,0\n" +
                ",0,0,0,0,0,0\n" +
                ",0,0,0,0,0,0\n" +
                ",0,0,0,0,0,0\n" +
                ",0,0,0,@upbCustomerGUID,@sUsage,@sYarnInfo\n" +
                ",0,0,@nYarnLength,@sMaterialTypeName,@sPatternNo,@sDtlQtyList\n" +
                ",@sProductWidth,@sProductGMWT,@sFinishingMethod,@sShelfNo,0,0\n" +
                "\n" +
                ",@nInLength,@nInWeight,@sWeightUnit,0,0,0\n" +
                ",0,0,0,0,0,0\n" +
                ",0,0,0,0,0,0\n" +
                ",@sCustomerSpecification,@nInNetQty,0)";
    }

    /**
     * 根据订单查询
     * @param params
     * @return
     * @throws SQLException
     */
    public Map getInStoreInfo(Map params) throws SQLException, ClassNotFoundException {
        ResultSet rs = JDBCUtils.queryData(getInStoreInfoSql(), "%"+params.get("orderNo")+"%");
        Map map = null;
        while(rs.next()){
            map = getMapResltByResultSet(rs,params);
        }
        return map;
    }

    private Map getMapResltByResultSet(ResultSet rs,Map params) throws SQLException {
        Map map = new HashMap<String,Object>();
        map.put("ummInHdrGUID",params.get("ummInHdrGUID"));
        map.put("ummMaterialGUID",rs.getString("ummMaterialGUID"));
        map.put("utmColorGUID",rs.getString("utmColorGUID"));
        map.put("usdOrderLotGUID",rs.getString("usdOrderLotGUID"));
        map.put("uRefDtlGUID",rs.getString("uRefDtlGUID"));

        map.put("sMaterialNo",rs.getString("sMaterialNo"));
        map.put("sMaterialName",rs.getString("sMaterialName"));
        map.put("sComponent",rs.getString("sComponent"));
        map.put("sColorNo",rs.getString("sColorNo"));
        map.put("sColorName",rs.getString("sColorName"));
        map.put("sMaterialLot",rs.getString("sMaterialLot"));

        map.put("sCardNo",rs.getString("sCardNo"));
        map.put("sGrade",rs.getString("sGrade"));
        map.put("sLocation",params.get("location")); //架位
        map.put("nInQty",rs.getBigDecimal("nInQty"));
        map.put("nInGrossQty",rs.getBigDecimal("nInGrossQty"));
        map.put("sUnit",rs.getString("sWeightUnit"));

        map.put("nInPkgQty",rs.getBigDecimal("nInPkgQty"));
        map.put("nInPkgUnitQty",rs.getBigDecimal("nInPkgUnitQty"));
        map.put("nIniPrice",rs.getBigDecimal("nIniPrice"));

        map.put("sOrderNo",rs.getString("sOrderNo"));
        map.put("sOrderColorNo",rs.getString("sOrderColorNo"));
        map.put("sCustomerOrderNo",rs.getString("sCustomerOrderNo"));

        map.put("sCustomerMaterialNo",rs.getString("sCustomerMaterialNo"));
        map.put("sRemark",rs.getString("sRemark"));
        map.put("nDiff04",rs.getBigDecimal("nDiff04"));
        map.put("sDiff29",rs.getString("sDiff29"));
        map.put("sDiff01",rs.getString("sDiff01"));

        map.put("upbCustomerGUID",rs.getString("upbCustomerGUID"));
        map.put("sUsage",rs.getString("sUsage"));
        map.put("sYarnInfo",rs.getString("sYarnInfo"));


        map.put("nYarnLength",rs.getBigDecimal("nYarnLength"));
        map.put("sMaterialTypeName",rs.getString("sMaterialTypeName"));
        map.put("sPatternNo",rs.getString("sPatternNo"));
        map.put("sDtlQtyList",rs.getString("sDtlQtyList"));

        map.put("sProductWidth",rs.getString("sProductWidth"));
        map.put("sProductGMWT",rs.getString("sProductGMWT"));
        map.put("sFinishingMethod",rs.getString("sFinishingMethod"));
        map.put("sShelfNo",params.get("shelfNo"));

        map.put("nInLength",rs.getBigDecimal("nInLength"));
        map.put("nInWeight",rs.getBigDecimal("nInWeight"));
        map.put("sWeightUnit",rs.getString("sWeightUnit"));

        map.put("sCustomerSpecification",rs.getString("sCustomerSpecification"));
        return map;
    }

    // 根据入库订单id更新入库信息信息
    public int modifyInStore(String inStoreNo) throws SQLException, ClassNotFoundException {
        return JDBCUtils.executeSql(updateInStoreSql(),inStoreNo);
    }

    /**
     * 新增入仓单
     * @param map
     * @return
     */
    public int insertInStore(Map map) throws SQLException, ClassNotFoundException {
        return JDBCUtils.executeSql(getInStoreSql(), map.get("inStoreNo"), map.get("userName"),map.get("userName"));
    }

    //查询所有带入仓的订单
    public ArrayList<Map> findAllPendingOrder() throws SQLException, ClassNotFoundException {
        ResultSet rs = JDBCUtils.queryData(getPendingOrderSql(""), null);

        ArrayList<Map> list = new ArrayList<>();

        while(rs.next()){
            Map<Object,Object> map = new HashMap<>();
            map.put("order_no",rs.getObject("order_no"));
            map.put("time",rs.getObject("time"));
            map.put("material_no",rs.getObject("material_no"));
            map.put("card_no",rs.getObject("Card_no"));
            map.put("quantity",rs.getObject("quantity"));
            map.put("selected",false);
            list.add(map);
        }

        try {
            JDBCUtils.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    //根据订单号查询所有的条码信息
    public List<Map> findInOrderDetailByOrderNo(String orderNo) throws SQLException, ClassNotFoundException {
        ResultSet rs = JDBCUtils.queryData(getInOrderDetailByOrderNoSql(), orderNo);
        return getInOrderDetailResultByResultSet(rs);
    }

    //根据缸号查询所有条码信息
    public List findWarehouseReceiptByCardNo(String cardNo) throws SQLException, ClassNotFoundException {
        ResultSet rs = JDBCUtils.queryData(getInOrderDetailByCardNoSql(), cardNo);
        return getInOrderDetailResultByResultSet(rs);
    }

    //根据条码号查询入仓信息
    public List findWarehouseReceiptByBarCode(String barCode) throws SQLException, ClassNotFoundException {
        //根据条码号查询订单号
        String orderNo = getCardNoByBarCode(barCode);
        if(StringUtils.isEmpty(orderNo)){
            return null;
        }
        return findInOrderDetailByOrderNo(orderNo);
    }

    // 生成入库单号
    public String getInStoreNo() throws SQLException, ClassNotFoundException {
        String sql = "DECLARE @sNewNoteNo VARCHAR(50)\n" +
                "EXEC dbo.sppbGenerateNoteNo N'INSTORENO', N'G02,I,,CZ,200912,T,2009', 1, @sNewNoteNo OUTPUT \n" +
                "SELECT @sNewNoteNo as sNewNoteNo";
        ResultSet rs = JDBCUtils.queryData(sql, null);
        String inStoreNo = "";
        while(rs.next()){
            inStoreNo = rs.getString("sNewNoteNo");
        }
        return inStoreNo;
    }


    /**
     * 根据ResultSet获取入仓信息
     * @param rs
     * @return
     */
    private List getInOrderDetailResultByResultSet(ResultSet rs) throws SQLException {
        List<Map> list = new ArrayList<>();
        while(rs.next()){
            Map<Object,Object> map = new HashMap<>();
            map.put("fabric_no",rs.getObject("barcode"));
            map.put("order_no",rs.getObject("store_in_no"));
//            map.put("quantity",rs.getObject("sequence"));
            map.put("card_no",rs.getObject("card_no"));
            map.put("weight",rs.getObject("weight"));
            map.put("unit",rs.getObject("sWeightUnit"));
            map.put("material_no",rs.getObject("sMaterialNo"));
            map.put("material_name",rs.getObject("sMaterialName"));
            map.put("time",rs.getObject("time"));
            map.put("scaned",false);
            list.add(map);
        }
        try {
            JDBCUtils.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    //根据缸号查询入仓信息sql
    private String getInOrderDetailByCardNoSql() throws SQLException {
        return "DECLARE @card_no nvarchar(50) = ?\n" +
                "\n" +
                "select h.[sStoreInNo] as [store_in_no]\n" +
                "\t, d.[sCardNo] as [card_no]\n" +
                "\t, d.sMaterialNo\n" +
                "\t, d.sMaterialName\n" +
                "\t, d.sMaterialLot\n" +
                "\t, sWeightUnit=d.sUnit\n" +
                "\t, bi.[sBarCode] as [barcode]\n" +
                "\t, bi.[nQty]\tas [weight]\n" +
                "\t, CONVERT(VARCHAR(20),bi.[tCreateTime],120) AS [time]\n" +
                "\tfrom [HSWeaveDyeingERP_TEST].[dbo].[mmSTInHdr] h with(nolock)\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmSTInDtl] d with(nolock) on h.[uGUID] = d.[ummInHdrGUID]\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmBarCodeIn] bi with(nolock) on d.uGUID = bi.[ummInDtlGUID]\t\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmStore] s with(nolock) on h.[ummStoreGUID] = s.[uGUID]\n" +
                "\tWHERE d.[sCardNo] = @card_no and s.[sStoreName] = '质检中转仓' \n" +
                "\tORDER by h.[sStoreInNo], bi.[sSeq]  ";
    }

    //根据条码查询订单号
    private String getCardNoByBarCode(String barCode) throws SQLException, ClassNotFoundException {
        String orderNo = "";
        ResultSet resultSet = JDBCUtils.queryData(getOrderNoByBarCodeSql(), barCode);
        while(resultSet.next()){
            orderNo = resultSet.getString("sStoreInNo");
        }
        try {
            JDBCUtils.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orderNo;
    }

    // 获取查询待入仓订单的sql
    private String getPendingOrderSql(String condition){
        return "set rowcount 0\n" +
                "select s.[sStoreName]\n" +
                "\t, d.[uGUID]\t\n" +
                "\t, h.[sStoreInNo]\n" +
                "\t, h.[sStockType]\n" +
                "\t, t.[sStoreInType]\n" +
                "\t, h.[tStoreInTime]\n" +
                "\t, datediff(minute, h.tStoreInTime, h.[tPrintTime]) diff_minutes\n" +
                "\t, h.[tPrintTime]\t\n" +
                "\t, d.[sMaterialNo]\n" +
                "\t, d.[sMaterialName]\n" +
                "\t, d.[sColorNo]\n" +
                "\t, d.[sColorName]\n" +
                "\t, d.[sCardNo]\n" +
                "\t, d.[nInPkgQty]\n" +
                "\t, d.[nInQty]\t\n" +
                "\t, d.[nStockPkgQty]\n" +
                "\t, d.[sCustomerOrderNo]\n" +
                "\t, d.[sProductWidth]\n" +
                "\t, d.[sProductGMWT]\n" +
                "\tinto #t1\n" +
                "\tfrom [HSWeaveDyeingERP_TEST].[dbo].[mmSTInHdr] h with(nolock)\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmStore] s with(nolock) on h.[ummStoreGUID] = s.[uGUID]\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmSTInDtl] d with(nolock) on h.[uGUID] = d.[ummInHdrGUID]\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmStoreInType] t with(nolock) on h.[immStoreInTypeID] = t.[iID]\n" +
                "\twhere h.[tStoreInTime] > dateadd(month, -1, getdate())\n" +
                "\t\tand s.[sStoreName] = '质检中转仓'\n" +
                "\t\tand d.[sCardNo] not like 'B[FR]%'\t\t--BF,RF开头的缸号，不允许入正品仓\n" +
                "\t\tand [h].[iStoreInStatus] = 1\n" +
                condition +
                "\n" +
                "select t.[uGUID]\n" +
                "\t, t.[sCardNo]\n" +
                "\t, sum(d.[nInPkgQty]) as nInPkgQty\n" +
                "\t, sum(d.[nInQty]) as nInQty\n" +
                "\tinto #t2\n" +
                "\tfrom [#t1] t\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmSTInDtl] d with(nolock) on t.uGUID = d.[uRefDtlGUID] and t.sCardNo = d.[sCardNo]\t\n" +
                "\tgroup by t.[uGUID], t.[sCardNo]\n" +
                "\n" +
                "update t1\n" +
                "\tset t1.nInQty = t1.nInQty - t2.[nInQty]\n" +
                "\t, t1.nInPkgQty = t1.nInPkgQty - t2.[nInPkgQty]\n" +
                "\tfrom #t1 t1\n" +
                "\tjoin #t2 t2 on t1.[uGUID] = t2.[uGUID]\n" +
                "\n" +
                "delete from #t1 where [nInPkgQty] <= 0\n" +
                "\n" +
                "select \n" +
                "\t[sStoreInNo] as [order_no]\n" +
                "\t, isnull(CONVERT(varchar(11),[tprinttime],111), CONVERT(varchar(11),[tStoreInTime],111)) as [time]\n" +
                "\t, [sMaterialNo] as [material_no]\n" +
                "\t, [sCardNo] as [Card_no]\n" +
                "\t, [nInPkgQty] as [quantity]\n" +
                "\t, [sColorName] as [color_name]\n" +
                "\tfrom [#t1]\n" +
                "\torder by [time]\n" +
                "\n" +
                "\n" +
                "drop table [#t1], [#t2]";
    }

    // 获取根据订单号查询布号（条码）明细sql
    private String getInOrderDetailByOrderNoSql(){
        return "DECLARE @order_no nvarchar(50) = ?\n" +
                "\n" +
                "select h.[sStoreInNo] as [store_in_no]\n" +
                "\t, d.[sCardNo] as [card_no]\n" +
                "\t, d.sMaterialNo\n" +
                "\t, d.sMaterialName\n" +
                "\t, d.sMaterialLot\n" +
                "\t, sWeightUnit=d.sUnit\n" +
                "\t, bi.[sSeq] as [sequence]\n" +
                "\t, bi.[sBarCode] as [barcode]\n" +
                "\t, bi.[nQty]\tas [weight]\n" +
                "\t, CONVERT(VARCHAR(20),bi.[tCreateTime],120)\tas [time]\n" +
                "\tfrom [HSWeaveDyeingERP_TEST].[dbo].[Split](@order_no, ',') o\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmSTInHdr] h with(nolock) on o.[Value] = h.[sStoreInNo]\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmSTInDtl] d with(nolock) on h.[uGUID] = d.[ummInHdrGUID]\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmBarCodeIn] bi with(nolock) on d.uGUID = bi.[ummInDtlGUID]\t\n" +
                "\torder by h.[sStoreInNo], bi.[sSeq]";
    }

    //根据条码（布号）查询订单号sql
    private String getOrderNoByBarCodeSql(){
        return "select h.[sStoreInNo]\n" +
                "\tfrom [HSWeaveDyeingERP_TEST].[dbo].[mmSTInHdr] h with(nolock)\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmSTInDtl] d with(nolock) on h.[uGUID] = d.[ummInHdrGUID]\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmBarCodeIn] bi with(nolock) on d.uGUID = bi.[ummInDtlGUID]\t\n" +
                "\tjoin [HSWeaveDyeingERP_TEST].[dbo].[mmStore] s with(nolock) on h.[ummStoreGUID] = s.[uGUID]\n" +
                "\tWHERE bi.[sBarCode] = ? and s.[sStoreName] = '质检中转仓' \n" +
                "\tORDER by h.[sStoreInNo], bi.[sSeq] ";
    }

    // 添加入库单sql 入库号，登录人，登录人
    private String getInStoreSql(){
        return "INSERT INTO dbo.mmSTInHdr (ummStoreGUID\n" +
                "                       ,uRefSourceGUID,sStoreInNo\n" +
                "                       ,immStoreInTypeID,iStoreInStatus,iSourceType,iBillType\n" +
                "                       ,sStoreInMan,tStoreInTime,bIsSystem\n" +
                "                       ,sCreator,tCreateTime,sUpdateMan,tUpdateTime\n" +
                "                       ,bIsDireOut,sStockType) \n" +
                "VALUES (N'{178E363C-BEEF-4D26-B571-9F0000FE919E}'\n" +
                "\t,N'{25C799D6-F279-4A9D-8E6B-A4C4009250E7}',?\n" +
                "    ,20,0,3,3\n" +
                "    ,N'谢绍华',CONVERT(VARCHAR(20),GETDATE(),120),0\n" +
                "    ,?,CONVERT(VARCHAR(20),GETDATE(),120),?,CONVERT(VARCHAR(20),GETDATE(),120)\n" +
                "    ,0,N'广州长丰'\n" +
                ")";
    }

    //更新入库订单信息sql
    private String updateInStoreSql(){
        return "DECLARE @ummInHdrGUID UNIQUEIDENTIFIER = ?, @immStoreInTypeID TINYINT\n" +
                "\n" +
                "-- 根据入库id，查询入库类型id\n" +
                "SELECT @immStoreInTypeID=A.immStoreInTypeID\n" +
                "FROM dbo.mmSTInHdr AS A WITH(NOLOCK)\n" +
                "WHERE A.uGUID=@ummInHdrGUID\n" +
                "\n" +
                "\n" +
                "----控制架位不能为空\n" +
                "IF EXISTS(\n" +
                "\tSELECT TOP 1 1 \n" +
                "\tFROM dbo.mmSTInHdr AS A WITH(NOLOCK)\n" +
                "\tWHERE A.uGUID=@ummInHdrGUID AND ISNULL(A.sStockType,'')='')\n" +
                "BEGIN\n" +
                "\tRAISERROR(N'账户为空,请务必正确选择账户.',16,1)\n" +
                "\tRETURN\n" +
                "END\n" +
                "\n" +
                "-- 更新客户公司名字\n" +
                "UPDATE  A\n" +
                "SET   sCarDriver = ( SELECT TOP 1\n" +
                "                                 B.sCustomerOrderNo\n" +
                "                       FROM     dbo.mmSTInDtl B\n" +
                "                       WHERE    B.ummInHdrGUID = A.uGUID\n" +
                "                     ) \n" +
                "FROM    dbo.mmSTInHdr AS A WITH ( NOLOCK )\n" +
                "WHERE   A.uGUID = @ummInHdrGUID \n" +
                "\n" +
                "-- 计算值病更新\n" +
                "UPDATE A\n" +
                "SET nDiff03=CONVERT(DECIMAL(18,2), ((A.nInQty-(A.nInPkgQty*(ISNULL(A.nYarnLength,0)+ISNULL(A.nDiff02,0) )) ) /A.nInNetQty -1)*100   )\n" +
                "FROM dbo.mmSTInDtl A \n" +
                "WHERE A.ummInHdrGUID=@ummInHdrGUID AND A.nInNetQty>0\n" +
                "\n" +
                "-- 跟新入库记录的库存类型\n" +
                "UPDATE a  SET sStockTypeNEW=c.sStockType\n" +
                "FROM  dbo.mmSTInLog A WITH(UPDLOCK) \n" +
                "INNER JOIN dbo.mmSTInDtl B WITH(UPDLOCK)  ON B.uGUID=A.ummSTInGuid\n" +
                "INNER JOIN dbo.mmSTInHdr C WITH(UPDLOCK)  ON C.uGUID = B.ummInHdrGUID\n" +
                "WHERE C.uGUID=@ummInHdrGUID\n" +
                "\n";
    }

    //根据订单号查询入库信息
    private String getInStoreInfoSql(){
        return "-- 根据缸号查询入仓信息 详细版本\n" +
                "\n" +
                "IF OBJECT_ID('TEMPDB..#Temp') IS NOT NULL DROP TABLE #Temp --判断是否存在表，存在删除\n" +
                "\n" +
                "SELECT TOP 50\n" +
                "bSelected=CAST(0 AS BIT),\n" +
                "C.[sStoreInNo] as [store_in_no], --订单号\n" +
                "L.sCardNo, --缸号\n" +
                "L.sColorNo, --色号\n" +
                "LEFT(L.sCardNo,2) AS sCardNoLEFT,\n" +
                "L.sColorName,\t--颜色名称\n" +
                "L.sMaterialNo, --物料编号\n" +
                "L.sMaterialName, --物料名称\n" +
                "L.nInWeight, -- 入仓重量\n" +
                "L.nInLength,\n" +
                "L.nInQty, --入库数量\n" +
                "L.nIniPrice, \n" +
                "L.nInGrossQty, \n" +
                "--L.nInRawQty,\n" +
                "sWeightUnit=L.sUnit, --重量单位\n" +
                "L.nInPkgQty, --条数\n" +
                "L.nInPkgUnitQty, --条数\n" +
                "L.sYarnInfo,  --纱支信息\n" +
                "L.sComponent, --成份\n" +
                "L.sMaterialTypeName, --物料类别\n" +
                "L.sProductWidth, --成品门幅\n" +
                "L.sProductGMWT, --成品克重\n" +
                "L.sFinishingMethod,  --整理方式\n" +
                "L.nYarnLength,\n" +
                "L.nDiff02,\n" +
                "L.nDiff04,\n" +
                "L.sMaterialLot,\t--批号\n" +
                "L.sGrade, --等级\n" +
                "L.sDiff01,\n" +
                "L.sPatternNo, --花型号\n" +
                "L.sDtlQtyList, --Dtl数量清单\n" +
                "sOrderNo=H.sRefOrderNo, --订单号\n" +
                "sOrderTime=CONVERT(VARCHAR(20),H.tCreateTime,120),\n" +
                "H.sOrderType AS sDiff29, --订单类别\n" +
                "HC.sCustomerNo,\t--客户编号\n" +
                "HC.sCustomerName,\t--客户名称\n" +
                "sOrderColorNo=D.sColorNo, --客户色号\n" +
                "H.sCustomerOrderNo,\t--客户订单号\n" +
                "D.sCustomerMaterialNo, --老货号\n" +
                "D.sCustomerMaterialName,\t--客户品名\n" +
                "D.sCustomerSpecification,\t--客户规范\n" +
                "D.sUsage,\n" +
                "sRemark=C.sRemark,\t--备注\n" +
                "L.sUnit, \t--数量单位\n" +
                "L.ummMaterialGUID, --物流材料id\n" +
                "L.utmColorGUID, --颜色id\n" +
                "L.usdOrderLotGUID,\t--订单数量id\n" +
                "H.upbCustomerGUID,\n" +
                "L.ummInHdrGUID,\n" +
                "uGUID=dbo.fnpbNewCombGUID(),\n" +
                "uRefDtlGUID=L.uGUID\t--uRefDtlGUID\n" +
                ",CONVERT(NVARCHAR(20),'') AS sLocation\n" +
                "INTO #Temp\n" +
                "FROM dbo.mmSTInHdr AS C WITH(NOLOCK)\n" +
                "INNER JOIN dbo.mmSTInDtl AS L WITH(NOLOCK) ON C.uGUID=L.ummInHdrGUID\n" +
                "INNER JOIN dbo.sdOrderLot F ON F.uGUID=L.usdOrderLotGUID\n" +
                "INNER JOIN dbo.sdOrderDtl AS D WITH(NOLOCK) ON F.usdOrderDtlGUID=D.uGUID\n" +
                "INNER JOIN dbo.sdOrderHdr AS H WITH(NOLOCK) ON D.usdOrderHdrGUID=H.uGUID\n" +
                "INNER JOIN dbo.psWorkFlowCard AS W WITH(NOLOCK) ON W.sCardNo=L.sCardNo\n" +
                "LEFT JOIN dbo.tmColor AS T WITH(NOLOCK) ON W.utmColorGUID=T.uGUID\n" +
                "LEFT JOIN dbo.pbSales AS PS WITH(NOLOCK) ON H.upbSalesGUID=PS.uGUID\n" +
                "LEFT JOIN dbo.sdOrderType AS ST WITH(NOLOCK) ON H.sOrderType=ST.sOrderType\n" +
                "LEFT JOIN dbo.pbCustomer AS HC WITH(NOLOCK) ON H.upbCustomerGUID=HC.uGUID\n" +
                "LEFT JOIN dbo.mmStore HY WITH(NOLOCK) ON HY.uGUID=C.ummStoreGUID\n" +
                "WHERE (((C.[sStoreInNo] LIKE ?))) AND HY.sStoreNo='G026'  --仓库为质检中转仓\n" +
                "AND C.iStoreInStatus=1\n" +
                "-- C.[sStoreInNo] LIKE N'%OS20037281%'\n" +
                "-- L.sCardNo LIKE '%BD200811028%'\n" +
                "\n" +
                "\n" +
                "-- 修改输入数量\n" +
                "\n" +
                "UPDATE BB\n" +
                "SET nInQty = BB.nInQty - AA.nStockQty\t--修改入库数量\n" +
                ",nInPkgQty = BB.nInPkgQty - AA.nStockPkgQty\t--条数\n" +
                "FROM(\n" +
                "SELECT A.uRefDtlGUID,nStockQty=SUM(B.nInQty)\n" +
                ",nStockPkgQty=SUM(B.nInPkgQty)\n" +
                "FROM #Temp A\n" +
                "JOIN dbo.mmSTinDtl B WITH(NOLOCK) ON B.sCardNo = A.sCardNo and b.uRefDtlGUID=a.uRefDtlGUID\n" +
                "JOIN dbo.mmSTinHdr C WITH(NOLOCK) ON C.uGUID = B.ummInHdrGUID \n" +
                "WHERE NOT EXISTS(SELECT TOP 1 1 FROM dbo.mmSTOutDtl A1  WITH(NOLOCK)\n" +
                "                  INNER JOIN dbo.mmSTOutHdr B1 WITH(NOLOCK) ON B1.uGUID = A1.ummOutHdrGUID\n" +
                "\t\t\t\t  INNER JOIN dbo.mmStoreOutType C1 WITH(NOLOCK) ON C1.iID = B1.immStoreOutTypeID\n" +
                "\t\t\t\t  WHERE A1.ummInDtlGUID=B.uGUID AND C1.iConvertType=1  )\n" +
                "--WHERE C.ummStoreGUID='178E363C-BEEF-4D26-B571-9F0000FE919E'\n" +
                "GROUP BY A.uRefDtlGUID\n" +
                ") AA\n" +
                "JOIN #Temp BB ON BB.uRefDtlGUID = AA.uRefDtlGUID\n" +
                "\n" +
                "\n" +
                "-- 根据出库类型，判断是否能入正品仓\n" +
                "\n" +
                " IF EXISTS(SELECT TOP 1 1 FROM #Temp A \n" +
                "                            INNER JOIN dbo.psWorkFlowCard B1 WITH(NOLOCK)  ON B1.sCardNo=A.sCardNo\n" +
                "\t\t\t\t\t\t\tINNER JOIN dbo.psWorkFlowCardRoll C1 WITH(NOLOCK)  ON C1.upsWorkFlowCardGUID = B1.uGUID\n" +
                "                            INNER JOIN dbo.mmBarCodeOut D1 WITH(NOLOCK)  ON D1.uGUID=C1.ummInDtlGUID\n" +
                "\t\t\t\t\t\t\tINNER JOIN dbo.mmFPOutHdr E1 WITH(NOLOCK)  ON E1.uGUID=D1.ummOutHdrGUID\n" +
                "\t\t\t\t\t\t\tWHERE E1.immStoreOutTypeID=21  AND E1.sCarIdentity=B1.sCardNo\n" +
                "\t\t\t\t\t\t\t)\n" +
                "BEGIN\n" +
                "\tRAISERROR(N'返修缸号，不允许入正品仓.',16,1)\n" +
                "\tRETURN\n" +
                "END\n" +
                "\n" +
                "IF EXISTS(SELECT TOP 1 1 FROM #Temp WHERE sCardNoLEFT IN ('RF','BF')  )\n" +
                "BEGIN\n" +
                "\tRAISERROR(N' BF,RF开头的缸号，不允许入正品仓.',16,1)\n" +
                "\tRETURN\n" +
                "END\n" +
                "\n" +
                "-- 返回结果\n" +
                "\n" +
                "-- SELECT sOrderNo,SUM(nInPkgQty) AS [条数],MAX(sOrderTime) AS [时间],SUM(nInQty) AS [入库数量]\n" +
                "SELECT *\n" +
                "FROM #Temp\n" +
                "WHERE nInQty>0\n" +
                "-- GROUP BY sOrderNo\n" +
                "\n" +
                "-- 删除临时表\n" +
                "IF OBJECT_ID('TEMPDB..#Temp') IS NOT NULL DROP TABLE #Temp\n";
    }

    public List getListByMap(Map map, Object location, Object shelfNo){
        List list = new ArrayList();
        list.add(map.get("ummInHdrGUID"));
        list.add(map.get("ummMaterialGUID"));
        list.add(map.get("utmColorGUID"));
        list.add(map.get("usdOrderLotGUID"));
        list.add(map.get("uRefDtlGUID"));

        list.add(map.get("sMaterialNo"));
        list.add(map.get("sMaterialName"));
        list.add(map.get("sComponent"));
        list.add(map.get("sColorNo"));
        list.add(map.get("sColorName"));
        list.add(map.get("sMaterialLot"));

        list.add(map.get("sCardNo"));
        list.add(map.get("sGrade"));
        list.add(location);
        list.add(map.get("nInQty"));
        list.add(map.get("nInGrossQty"));
        list.add(map.get("sUnit"));

        list.add(map.get("nInPkgQty"));
        list.add(map.get("nInPkgUnitQty"));
        list.add(map.get("nInPkgExQty"));
        list.add(map.get("nIniPrice"));

        list.add(map.get("sOrderNo"));
        list.add(map.get("sOrderColorNo"));
        list.add(map.get("sCustomerOrderNo"));

        list.add(map.get("sCustomerMaterialNo"));
        list.add(map.get("sRemark"));
        list.add(map.get("sDiff01"));
        list.add(map.get("sDiff29"));

        list.add(map.get("nDiff04"));

        list.add(map.get("upbCustomerGUID"));
        list.add(map.get("sUsage"));
        list.add(map.get("sYarnInfo"));

        list.add(map.get("nYarnLength"));
        list.add(map.get("sMaterialTypeName"));
        list.add(map.get("sPatternNo"));
        list.add(map.get("sDtlQtyList"));

        list.add(map.get("sProductWidth"));
        list.add(map.get("sProductGMWT"));
        list.add(map.get("sFinishingMethod"));
        list.add(shelfNo);

        list.add(map.get("nInLength"));
        list.add(map.get("nInWeight"));
        list.add(map.get("sWeightUnit"));

        list.add(map.get("sCustomerSpecification"));
        list.add(map.get("nInNetQty"));
        return list;
    }
}
