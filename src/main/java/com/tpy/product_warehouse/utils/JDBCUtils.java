package com.tpy.product_warehouse.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JDBCUtils {

//
//    @Value("${spring.datasource.driver-class-name}")
    private static String DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
//    =
//    @Value("${spring.datasource.url}")
    private static String URL = "jdbc:sqlserver://sycfgroup.com:10000;DatabaseName=HSWeaveDyeingERP_TEST";
//    private static String URL = "jdbc:sqlserver://localhost:1433;DatabaseName=TESTDB";
//    @Value("${spring.datasource.username}")
    private static String USER_NAME = "tangpengyi";
//    @Value("${spring.datasource.password}")
    private static String PASSWORD = "tang8888";

    private static Connection conn = null;
    private static PreparedStatement ps = null;
    private static ResultSet rs = null;

    public static Connection getConn() throws ClassNotFoundException, SQLException {
        if(conn == null){
            Class.forName(DRIVER);
            conn = DriverManager.getConnection(URL,USER_NAME,PASSWORD);
        }
        return conn;
    }

    //查询数据，返回数据集
    public static ResultSet queryData(String sSql,Object ... params) throws SQLException, ClassNotFoundException {
        getConn();
        try {
            //3.发送sql语句
            ps = conn.prepareStatement(sSql);
            if(params != null){
                for(int i = 1; i <= params.length; i++){
                    ps.setObject(i,params[i-1]);
                }
            }

            //4.执行
            return ps.executeQuery();
        } catch (Exception e) {
            log.info(e.getMessage());
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
    public static int executeSql(String sSql,Object ... params) throws SQLException, ClassNotFoundException {

        getConn();

        PreparedStatement ps = null;
        int rs = 0;
        try {
            ps = conn.prepareStatement(sSql);
            for(int i = 1; i <= params.length; i++){
                ps.setObject(i,params[i-1]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            log.error(e.getMessage());
            return rs;
        }
    }

    public static int executeSqlByList(String sSql, List params) throws SQLException, ClassNotFoundException {

        getConn();

        PreparedStatement ps = null;
        int rs = 0;
        try {
            ps = conn.prepareStatement(sSql);
            if(params != null){
                for(int i = 1; i <= params.size(); i++){
                    ps.setObject(i,params.get(i-1));
                }
            }

            return ps.executeUpdate();
        } catch (SQLException e) {
            log.error(e.getMessage());
            return rs;
        }
    }


    public static ResultSet querySqlByList(String sSql, List params) throws SQLException, ClassNotFoundException {

        getConn();

        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sSql);
            if(params != null){
                for(int i = 1; i <= params.size(); i++){
                    ps.setObject(i,params.get(i-1));
                }
            }

            return ps.executeQuery();
        } catch (SQLException e) {
            log.error(e.getMessage());
            return null;
        }
    }


    /**
     *
     * @param sSql
     * @param map key就是他们的注入参数的顺序，value就是参数值
     * @return
     */
    public static ResultSet executeQueryMap(String sSql,Map map) throws SQLException, ClassNotFoundException {
        getConn();
        try {
            //3.发送sql语句
            ps = conn.prepareStatement(sSql);
            if(map != null){
                for(int i = 1; i <= map.size(); i++){
                    ps.setObject(i,map.get(i));
                }
            }
            return rs;
        } catch (Exception e) {
            log.info(e.getMessage());
            return null;
        }
    }

    public static void close() throws SQLException {

        if(rs != null)
            rs.close();
        if(ps != null)
            ps.close();
    }
}