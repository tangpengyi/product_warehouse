package com.tpy.product_warehouse.dao;

import com.tpy.product_warehouse.utils.JDBCUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InOrderDao {

    @Autowired
    private JDBCUtils jdbcUtils;

    public Map getInStoreIfno(String orderNo) throws SQLException {
        ResultSet rs = jdbcUtils.queryData(getInStoreIfnoSql(), orderNo);
        Map map = null;
        while(rs.next()){


        }

        return null;
    }

    private Map getMapResltByResultSet(ResultSet rs) throws SQLException {
        Map map = new HashMap<String,Object>();
        map.put("ummInHdrGUID",rs.getString("ummInHdrGUID"));
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
        //架位
        map.put("sLocation","架位");
        map.put("nInQty",rs.getString("nInQty"));
        map.put("nInGrossQty",rs.getString("nInGrossQty"));
        map.put("sUnit",rs.getString("sWeightUnit"));

        map.put("nInPkgQty",rs.getString("nInPkgQty"));
        map.put("nInPkgUnitQty",rs.getString("nInPkgUnitQty"));
        map.put("nInPkgExQty",rs.getString("nInPkgExQty")); //数据对不上
        map.put("nIniPrice",rs.getString("nIniPrice"));
        map.put("nACPrice",0);
        map.put("nAmount",0);

        map.put("sOrderNo",rs.getString("sOrderNo"));
        map.put("sOrderColorNo",rs.getString("sOrderColorNo"));
        map.put("sCustomerOrderNo",rs.getString("sCustomerOrderNo"));

        map.put("sCustomerMaterialNo",rs.getString("sCustomerMaterialNo"));
        map.put("sRemark",rs.getString("sRemark"));
        map.put("nDiff04",rs.getString("nDiff04"));

        map.put("upbCustomerGUID",rs.getString("upbCustomerGUID"));
        map.put("sUsage",rs.getString("sUsage"));
        map.put("sYarnInfo",rs.getString("sYarnInfo"));

        map.put("nYarnLength",rs.getString("nYarnLength"));
        map.put("sMaterialTypeName",rs.getString("sMaterialTypeName"));
        map.put("sPatternNo",rs.getString("sPatternNo"));
        map.put("sDtlQtyList",rs.getString("sDtlQtyList"));

        map.put("sProductWidth",rs.getString("sProductWidth"));
        map.put("sProductGMWT",rs.getString("sProductGMWT"));
        map.put("sFinishingMethod",rs.getString("sFinishingMethod"));
        map.put("nInRawQty",rs.getString("sDtlQtyList"));

        map.put("nInLength",rs.getString("nInLength"));
        map.put("nInWeight",rs.getString("nInWeight"));
        map.put("sWeightUnit",rs.getString("sWeightUnit"));
        map.put("nInRawQty",rs.getString("sDtlQtyList"));

        map.put("sCustomerSpecification",rs.getString("sCustomerSpecification"));
        map.put("nInNetQty,",rs.getString("nInNetQty,"));
        return map;
    }

    // 根据入库订单id更新入库信息信息
    public int modifyInStore(String inStoreNo){
        return jdbcUtils.executeSql(updateInStoreSql(),inStoreNo);
    }

    /**
     * 新增入仓单
     * @param map
     * @return
     */
    public int insertInStore(Map map){
        return jdbcUtils.executeSql(getInStoreSql(), map.get("inStoreNo"), map.get("loginName"),map.get("loginName"));
    }

    //查询所有带入仓的订单
    public ArrayList<Map> findAllPendingOrder() throws SQLException {
        ResultSet rs = jdbcUtils.queryData(getPendingOrderSql(""), null);

        ArrayList<Map> list = new ArrayList<>();

        while(rs.next()){
            Map<Object,Object> map = new HashMap<>();
            map.put("order_no",rs.getObject("order_no"));
            map.put("time",rs.getObject("time"));
            map.put("material_no",rs.getObject("material_no"));
            map.put("card_no",rs.getObject("Card_no"));
            map.put("quantity",rs.getObject("quantity"));
            list.add(map);
        }

        try {
            jdbcUtils.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    //根据订单号查询所有的条码信息
    public List<Map> findInOrderDetailByOrderNo(String orderNo) throws SQLException {
        ResultSet rs = jdbcUtils.queryData(getInOrderDetailByOrderNoSql(), orderNo);
        return getInOrderDetailResultByResultSet(rs);
    }

    //根据缸号查询所有条码信息
    public List findWarehouseReceiptByCardNo(String cardNo) throws SQLException {
        ResultSet rs = jdbcUtils.queryData(getInOrderDetailByCardNoSql(), cardNo);
        return getInOrderDetailResultByResultSet(rs);
    }

    //根据条码号查询入仓信息
    public List findWarehouseReceiptByBarCode(String barCode) throws SQLException {
        //根据条码号查询订单号
        String orderNo = getCardNoByBarCode(barCode);
        if(StringUtils.isEmpty(orderNo)){
            return null;
        }
        return findInOrderDetailByOrderNo(orderNo);
    }

    // 生成入库单号
    public String getInStoreNo() throws SQLException {
        String sql = "DECLARE @sNewNoteNo VARCHAR(50)\n" +
                "EXEC dbo.sppbGenerateNoteNo N'INSTORENO', N'G02,I,,CZ,200912,T,2009', 1, @sNewNoteNo OUTPUT \n" +
                "SELECT @sNewNoteNo as sNewNoteNo";
        ResultSet rs = jdbcUtils.queryData(sql, null);
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
            list.add(map);
        }
        try {
            jdbcUtils.close();
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
    private String getCardNoByBarCode(String barCode) throws SQLException {
        String orderNo = "";
        ResultSet resultSet = jdbcUtils.queryData(getOrderNoByBarCodeSql(), barCode);
        while(resultSet.next()){
            orderNo = resultSet.getString("sStoreInNo");
        }
        try {
            jdbcUtils.close();
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

    //根据条码（布号）查询缸号sql
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
        return "DECLARE @ummInHdrGUID UNIQUEIDENTIFIER, @immStoreInTypeID TINYINT\n" +
                "SELECT @ummInHdrGUID=uGUID\n" +
                "FROM dbo.mmSTInHdr\n" +
                "WHERE sStoreInNo = ?\n" +
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
    private String getInStoreIfnoSql(){
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
                "L.nIniPrice,, \n" +
                "L.nInPkgExQty,\n" +
                "L.nInNetQty,\n" +
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
                "D.sCustomerMaterialNo, --客户品号\n" +
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
}
