package com.tpy.product_warehouse.filer;

import com.tpy.product_warehouse.utils.JWTUtls;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.SignatureException;

@Component
public class TokenInterceptor extends HandlerInterceptorAdapter {


    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws SignatureException {

        /** Token 验证 */
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new Exception("用户未登录");
            }

            String token = authHeader.substring(7);
            //jjwt验证
            JWTUtls.verity(token);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
