package com.tpy.product_warehouse.controller;

import com.tpy.product_warehouse.api.ResponseResult;
import com.tpy.product_warehouse.utils.JDBCUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
