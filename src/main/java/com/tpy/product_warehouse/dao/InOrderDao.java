package com.tpy.product_warehouse.dao;

import com.tpy.product_warehouse.api.CommonsResult;
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
        String sql = getPendingOrderSql();
        ResultSet rs = jdbcUtils.queryData(sql, null);

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
        String materialNo = "";
        String materialName = "";
        String weightUnit = "";
        ResultSet resultSet = jdbcUtils.queryData(getFindCardNoByOrderNo(), "%"+orderNo+"%");
        while(resultSet.next()){
            cardNo = resultSet.getString("sCardNo");
            materialNo = resultSet.getString("sMaterialNo");
            materialName = resultSet.getString("sMaterialName");
            weightUnit = resultSet.getString("sWeightUnit");
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
            map.put("fabric_no",rs.getObject("sSeq"));
            map.put("order_no",orderNo);
            map.put("fabric_no",rs.getObject("material_no"));
            map.put("quantity",rs.getObject("nNetQty"));
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

    // 获取查询待入仓订单的sql
    private String getPendingOrderSql(){
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
                "\tfrom [#t1]\n" +
                "\torder by [time]\n" +
                "\n" +
                "\n" +
                "drop table [#t1], [#t2]";
    }

    // 获取根据缸号查询布号（条码）明细sql
    private String getInOrderDetailSql(){
        return "SELECT B.sDiff30 -- '入库ID'" +
                ",  B.sCardNo -- '缸号'" +
                ", A.sBarCode -- '条码号'" +
                ", A.nNetQty -- '坯布数量'" +
                ",CONVERT(DECIMAL(18,2),A.nNetQty*2.2044) -- '坯布磅数'" +
                ",A.nLength -- '长度',\n" +
                "A.nPkgExQty -- '坯辅助数'" +
                ",  A.sSeq -- '布票号'" +
                ",A.nQty -- '数量'" +
                ",A.nQty -- '重量',\n" +
                "CONVERT(DECIMAL(18,2),A.nQty*2.2044) -- '磅数',\n" +
                "A.nPkgExQty -- '辅助数'" +
                ",a.nNetQty  -- '坯重'" +
                ",a.tCreateTime -- '创建时间'\n" +
                "FROM dbo.mmBarCodeIn A WITH(NOLOCK)\n" +
                "INNER JOIN dbo.mmSTInDtl B WITH(NOLOCK) ON B.uGUID=A.ummInDtlGUID\n" +
                "WHERE B.sCardNo='?' AND B.sDiff30 = ''\n" +
                "ORDER BY a.ummInDtlGUID,A.sSeq";
    }

    private String getFindCardNoByOrderNo(){
        return "SELECT L.sCardNo, --缸号\n" +
                "sOrderNo=H.sRefOrderNo --订单号\n" +
                "L.sMaterialNo, --物料编号\n" +
                "L.sMaterialName, --物料名称\n" +
                "sWeightUnit=L.sUnit --重量单位"+
                "FROM dbo.mmSTInHdr AS C WITH(NOLOCK)\n" +
                "INNER JOIN dbo.mmSTInDtl AS L WITH(NOLOCK) ON C.uGUID=L.ummInHdrGUID\n" +
                "INNER JOIN dbo.sdOrderLot F ON F.uGUID=L.usdOrderLotGUID\n" +
                "INNER JOIN dbo.sdOrderDtl AS D WITH(NOLOCK) ON F.usdOrderDtlGUID=D.uGUID\n" +
                "INNER JOIN dbo.sdOrderHdr AS H WITH(NOLOCK) ON D.usdOrderHdrGUID=H.uGUID\n" +
                "LEFT JOIN dbo.mmStore HY WITH(NOLOCK) ON HY.uGUID=C.ummStoreGUID\n" +
                "WHERE ((( H.sRefOrderNo LIKE N'?'))) AND HY.sStoreNo='G026'  --缸号 模糊查询\n" +
                "AND C.iStoreInStatus=1";
    }
}
