package com.tpy.product_warehouse.utils;

import com.tpy.product_warehouse.api.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Map;

@Component
public class JDBCUtils {

    @Autowired
    private DataSource dataSource;

    private static PreparedStatement ps = null;
    private static ResultSet rs = null;


    //查询数据，返回数据集
    public ResultSet queryData(String sSql,Object ... params) {
        try {
            //3.发送sql语句
            ps = dataSource.getConnection().prepareStatement(sSql);
            if(params != null){
                for(int i = 1; i <= params.length; i++){
                    ps.setObject(i,params[i-1]);
                }
            }

            //4.执行
            rs = ps.executeQuery();
            return rs;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return null;
            // TODO: handle exception
        }
    }

    /**
     *  执行SQL语句，比如update、delete
     * @param sSql
     * @param params 需要传入参数，这些参数必须严格按照注入参数的顺序
     * @return
     */
    public int executeSql(String sSql,Object ... params) {

        PreparedStatement ps = null;
        int rs = 0;
        try {
            ps = dataSource.getConnection().prepareStatement(sSql);
            for(int i = 1; i <= params.length; i++){
                ps.setObject(i,params[i-1]);
            }

            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return rs;
        } finally {
            try {
                close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * @param sSql
     * @param map key就是他们的注入参数的顺序，value就是参数值
     * @return
     */
    public ResultSet executeQueryMap(String sSql,Map map) {

        try {
            //3.发送sql语句
            ps = dataSource.getConnection().prepareStatement(sSql);
            if(map != null){
                for(int i = 1; i <= map.size(); i++){
                    ps.setObject(i,map.get(i));
                }
            }

            //4.执行
            rs = ps.executeQuery();
            //循环取出雇员的名字，薪水，部门的编号
//            while (rs.next()) {
//                String color_no = rs.getString("sSql");
//                System.out.println("===="+color_no);
//            }
            return rs;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return null;
            // TODO: handle exception
        }
    }

    public void close() throws SQLException {
        if(ps != null)
            ps.close();

        if(rs != null)
            rs.close();
    }
}