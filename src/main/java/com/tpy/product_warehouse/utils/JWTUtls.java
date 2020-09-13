package com.tpy.product_warehouse.utils;

import lombok.extern.slf4j.Slf4j;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import javax.servlet.http.HttpServletRequest;

@Slf4j
public class JWTUtls {

    private static final String SECERT = "123456123456123456";

    private static String token = "";

    /**
     * 验证token
     * @param token
     */
    public static DecodedJWT verity(String token){
        JWTUtls.token = token;
        return JWT.require(Algorithm.HMAC256(SECERT)).build().verify(token);
    }

    /**
     * 根据request查询用户id
     * @return
     */
    public static Integer getUserIdByRequest(){
        DecodedJWT verity = JWTUtls.verity(JWTUtls.token);
        String user_id = verity.getClaim("user_id").asString();
        return Integer.parseInt(user_id);
    }
}

