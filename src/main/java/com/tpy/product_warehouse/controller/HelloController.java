package com.tpy.product_warehouse.controller;

import com.tpy.product_warehouse.dao.InOrderDao;
import com.tpy.product_warehouse.utils.JDBCUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("test")
public class HelloController {

    @Resource
    private InOrderDao inOrderDao;

    @GetMapping("getNInNetQtyByBarCodes")
    public Map getNInNetQtyByBarCodes() throws SQLException, ClassNotFoundException {
        List list = new ArrayList();
        list.add("CF200823067001");
        list.add("CF200823067002");
        return inOrderDao.getNInNetQtyByBarCodes(list);
    }

    @GetMapping("inStoreProcedure")
    public int inStoreProcedure() throws SQLException, ClassNotFoundException {
        return 0;
    }

    @GetMapping("getGUID")
    public String getGUID() throws SQLException, ClassNotFoundException {
        return inOrderDao.getGUID();
    }


    @GetMapping("insertQuery")
    public String insertQuery() throws SQLException, ClassNotFoundException {
        ResultSet rs = JDBCUtils.queryData("declare @value varchar(20) = '姓名'\n" +
                "INSERT INTO t_user VALUES(@value,21)\n" +
                "SELECT @value as name",null);
        String name = null;
        while(rs.next()){
            name = rs.getString("name");
        }
        return name;
    }

    @GetMapping("uniqueidentifier")
    public String uniqueidentifierTest() throws SQLException, ClassNotFoundException {
        String guid = null;
        ResultSet rs = JDBCUtils.executeQueryMap("SELECT guid FROM test1_t",null);
        while (rs.next()) {
            guid = rs.getString("guid");
        }
        JDBCUtils.close();

        JDBCUtils.executeSql("INSERT INTO test2_t VALUES(?)", guid);
        JDBCUtils.close();
        return "成功";
    }


    @GetMapping("say")
    public String hello() throws SQLException, ClassNotFoundException {

        ResultSet rs = JDBCUtils.executeQueryMap("SELECT TOP 1 * FROM dbo.test", null);
        while (rs.next()) {
                String color_no = rs.getString("sSql");
            }
        return "成功";
    }

    @GetMapping("test")
    public String test(){
        return "hello world";
    }


    @RequestMapping("/file")
    @ResponseBody
    public void file(HttpServletRequest request, HttpServletResponse response) {
        String name = request.getParameter("app.apk.1");
        String path = "D:\\环思\\product_warehouse\\log\\app.apk.1";

        File imageFile = new File(path);
        if (!imageFile.exists()) {
            return;
        }
        //下载的文件携带这个名称
        response.setHeader("Content-Disposition", "attachment;filename=app.apk.1");
        //文件下载类型--二进制文件
        response.setContentType("application/octet-stream");

        try {
            FileInputStream fis = new FileInputStream(path);
            byte[] content = new byte[fis.available()];
            fis.read(content);
            fis.close();

            ServletOutputStream sos = response.getOutputStream();
            sos.write(content);

            sos.flush();
            sos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        List list = new ArrayList();
        list.add("1");
        list.add("2");
        list.add("3");
        System.out.println(getNInNetQtyByBarCodes(list));
    }

    public static String getNInNetQtyByBarCodes(List barCodes){

        StringBuffer param = new StringBuffer();
        int count = 0;
        for(Object barCode : barCodes){
            count ++;
            param.append(barCode);
            if(barCodes.size() != count){
                param.append(",'");
            }
        }
        return param.toString();
    }

}
