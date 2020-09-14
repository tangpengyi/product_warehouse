package com.tpy.product_warehouse.controller;

import com.tpy.product_warehouse.api.ResponseResult;
import com.tpy.product_warehouse.utils.JDBCUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;

@RestController
@RequestMapping("test")
public class HelloController {

    @Autowired
    private JDBCUtils jdbcUtils;

    @GetMapping("say")
    public String hello() throws SQLException {

        ResultSet rs = jdbcUtils.executeQueryMap("SELECT TOP 1 * FROM dbo.test", null);
        while (rs.next()) {
                String color_no = rs.getString("sSql");
                System.out.println("===="+color_no);
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

}
