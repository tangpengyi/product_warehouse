package com.tpy.product_warehouse.dao;

import com.tpy.product_warehouse.utils.JDBCUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
public class InOrderDao {

    @Autowired
    private JDBCUtils jdbcUtils;

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
    public ArrayList<Map> findInOrderDetailByOrderNo(String orderNo) throws SQLException {

        // 根据订单号查询缸号
        String cardNo = "";
        //材料编号
        String materialNo = "";
        //材料名
        String materialName = "";
        //重量单位
        String weightUnit = "";
        //批号
        String materialLot = "";
        ResultSet resultSet = jdbcUtils.queryData(getOrderNoByCardNoSql("H.sRefOrderNo LIKE ?"), "%"+orderNo+"%");
        while(resultSet.next()){
            cardNo = resultSet.getString("sCardNo");
            materialNo = resultSet.getString("sMaterialNo");
            materialName = resultSet.getString("sMaterialName");
            weightUnit = resultSet.getString("sWeightUnit");
            materialLot = resultSet.getString("sMaterialLot");
        }
        try {
            jdbcUtils.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if(StringUtils.isEmpty(cardNo)){
            return null;
        }

        // 根据缸号查询布号（条码）信息
        ResultSet rs = jdbcUtils.queryData(getInOrderDetailSql(), cardNo);
        ArrayList<Map> list = new ArrayList<>();

        while(rs.next()){
            Map<Object,Object> map = new HashMap<>();
            map.put("fabric_no",rs.getObject("sBarCode"));
            map.put("order_no",orderNo);
            map.put("quantity",rs.getObject("nNetQty"));
            map.put("batch_no",materialLot);
            map.put("weight",rs.getObject("nQty"));
            map.put("unit",weightUnit);
            map.put("material_no",materialNo);
            map.put("material_name",materialName);
            list.add(map);
        }

        try {
            jdbcUtils.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    //根据订单号查询入仓信息
    public Map findWarehouseReceiptByOrderNo(String orderNo) throws SQLException {
        ResultSet rs = jdbcUtils.queryData(getByCardNoSql("H.sRefOrderNo LIKE ?"), "%"+orderNo+"%");
        return getResultByResultSet(rs);
    }

    //根据缸号查询入仓信息
    //重量不知道从哪里来
    public Map findWarehouseReceiptByCardNo(String cardNo) throws SQLException {
        ResultSet rs = jdbcUtils.queryData(getByCardNoSql("L.sCardNo LIKE ?"), "%"+cardNo+"%");
        return getResultByResultSet(rs);
    }

    private Map getResultByResultSet(ResultSet rs) throws SQLException {
        Map<Object,Object> map = null;
        while(rs.next()){
            map = new HashMap<>();
            map.put("order_no",rs.getObject("sOrderNo"));
            map.put("time",rs.getObject("sOrderTime"));
            //入库数
            map.put("weight",rs.getObject("nInQty"));
            map.put("quantity",rs.getObject("nInPkgQty"));
        }

        try {
            jdbcUtils.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }


    //根据条码号查询入仓信息
    public Map findWarehouseReceiptByBarCode(String barCode) throws SQLException {
        String cardNo= getCardNoByBarCode(barCode);
        if(StringUtils.isEmpty(cardNo)){
            return null;
        }
        return null;
    }


    //根据缸号查询入仓信息sql
    private String getByCardNoSql(String contition) throws SQLException {
        return "-- 根据缸号查询入仓信息 详细版本\n" +
                "\n" +
                "IF OBJECT_ID('TEMPDB..#Temp') IS NOT NULL DROP TABLE #Temp --判断是否存在表，存在删除\n" +
                "\n" +
                "SELECT TOP 50\n" +
                "bSelected=CAST(0 AS BIT),\n" +
                "L.sCardNo, --缸号\n" +
                "L.sColorNo, --色号\n" +
                "LEFT(L.sCardNo,2) AS sCardNoLEFT,\n" +
                "L.sColorName,\t--颜色名称\n" +
                "L.sMaterialNo, --物料编号\n" +
                "L.sMaterialName, --物料名称\n" +
                "L.nInWeight, -- 入仓重量\n" +
                "L.nInQty, --入库数量\n" +
                "--L.nInRawQty,\n" +
                "sWeightUnit=L.sUnit, --重量单位\n" +
                "L.nInPkgQty, --条数\n" +
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
                "sOrderTime=CONVERT(VARCHAR(20),H.tCreateTime,120), --订单创建时间\n" +
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
                "WHERE ((( "+contition+"))) AND HY.sStoreNo='G026'  --缸号 模糊查询\n" +
                "AND C.iStoreInStatus=1\n" +
                "-- \n" +
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
                "SELECT * FROM #Temp\n" +
                "WHERE nInQty>0\n" +
                "\n" +
                "-- 删除临时表\n" +
                "IF OBJECT_ID('TEMPDB..#Temp') IS NOT NULL DROP TABLE #Temp\n";
    }

    //根据缸号查询订单号
    private String getCardNoByBarCode(String barCode) throws SQLException {
        String cardNp = "";
        ResultSet resultSet = jdbcUtils.queryData(getCardNoByBarCodeSql(), barCode);
        while(resultSet.next()){
            cardNp = resultSet.getString("sCardNo");
        }
        try {
            jdbcUtils.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cardNp;
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

    // 获取根据缸号查询布号（条码）明细sql
    private String getInOrderDetailSql(){
        /**
         * SELECT B.sDiff30 AS '入库ID',
         * B.sCardNo AS '缸号',
         * A.sBarCode AS '条码号',
         * A.nNetQty AS '坯布数量',
         * CONVERT(DECIMAL(18,2),A.nNetQty*2.2044) AS '坯布磅数'
         * ,A.nLength AS '长度',
         * A.nPkgExQty AS '坯辅助数'
         * ,  A.sSeq AS '布票号'
         * ,A.nQty AS '数量
         * ',A.nQty AS '重量'
         * ,CONVERT(DECIMAL(18,2),A.nQty*2.2044) AS '磅数'
         * ,A.nPkgExQty AS '辅助数'
         * ,a.nNetQty  AS '坯重'
         * ,a.tCreateTime as '创建时间'
         * FROM dbo.mmBarCodeIn A WITH(NOLOCK)
         * INNER JOIN dbo.mmSTInDtl B WITH(NOLOCK) ON B.uGUID=A.ummInDtlGUID
         * WHERE B.sCardNo='BS200809002' AND B.sDiff30 = ''
         * ORDER BY a.ummInDtlGUID,A.sSeq
         */
        return "SELECT B.sDiff30 \n" +
                ",  B.sCardNo \n" +
                ", A.sBarCode\n" +
                ", A.nNetQty \n" +
//                CONVERT(DECIMAL(18,2),A.nNetQty*2.2044) AS '坯布磅数'+
                ",CONVERT(DECIMAL(18,2),A.nNetQty*2.2044)\n" +
                ",A.nLength\n" +
                ",A.nPkgExQty\n" +
                ",  A.sSeq\n" +
                ",A.nQty\n" +
                ",CONVERT(DECIMAL(18,2),A.nQty*2.2044)\n" +
                ",A.nPkgExQty\n" +
                ",a.nNetQty\n" +
                ",a.tCreateTime\n" +
                "FROM dbo.mmBarCodeIn A WITH(NOLOCK)\n" +
                "INNER JOIN dbo.mmSTInDtl B WITH(NOLOCK) ON B.uGUID=A.ummInDtlGUID\n" +
                "WHERE B.sCardNo=? AND B.sDiff30 = ''\n" +
                "ORDER BY a.ummInDtlGUID,A.sSeq";
    }

    /**
     * 缸号 订单号相互查询
     * @param condition 查询条件
     * @return
     */
    private String getOrderNoByCardNoSql(String condition){
        return "SELECT TOP 50\n" +
                "L.sCardNo, --缸号\n" +
                "L.sMaterialNo,\n" +
                "L.sMaterialName,\n" +
                "L.sMaterialLot,\n" +
                "sWeightUnit=L.sUnit,\n" +
                "sOrderNo=H.sRefOrderNo --订单号\n" +
                "FROM dbo.mmSTInHdr AS C WITH(NOLOCK)\n" +
                "INNER JOIN dbo.mmSTInDtl AS L WITH(NOLOCK) ON C.uGUID=L.ummInHdrGUID\n" +
                "INNER JOIN dbo.sdOrderLot F ON F.uGUID=L.usdOrderLotGUID\n" +
                "INNER JOIN dbo.sdOrderDtl AS D WITH(NOLOCK) ON F.usdOrderDtlGUID=D.uGUID\n" +
                "INNER JOIN dbo.sdOrderHdr AS H WITH(NOLOCK) ON D.usdOrderHdrGUID=H.uGUID\n" +
                "LEFT JOIN dbo.mmStore HY WITH(NOLOCK) ON HY.uGUID=C.ummStoreGUID\n" +
                "WHERE ((("+condition+"))) AND HY.sStoreNo='G026'  --订单号 模糊查询\n" +
                "AND C.iStoreInStatus=1";
    }

    //根据条码（布号）查询缸号sql
    public String getCardNoByBarCodeSql(){
        return "SELECT B.sCardNo , A.sBarCode \n" +
                "FROM dbo.mmBarCodeIn A WITH(NOLOCK)\n" +
                "INNER JOIN dbo.mmSTInDtl B WITH(NOLOCK) ON B.uGUID=A.ummInDtlGUID\n" +
                "WHERE A.sBarCode=? AND B.sDiff30 = '' --条码号\n" +
                "ORDER BY a.ummInDtlGUID,A.sSeq";
    }
}
