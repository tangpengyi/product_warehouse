package com.tpy.product_warehouse.api;

public class CommonsResult {

    public static ResponseResult result = new ResponseResult();

    public static ResponseResult getSuccessResult(String msg){
        cleanResult();
        result.setCode(200);
        result.setMsg(msg);
        return result;
    }

    public static ResponseResult getSuccessResult(String msg,Object obj){
        cleanResult();
        result.setCode(200);
        result.setMsg(msg);
        result.setData(obj);
        return result;
    }

    public static ResponseResult getFialResult(String msg){
        cleanResult();
        result.setCode(500);
        result.setMsg(msg);
        return result;
    }

    public static ResponseResult getFialResult(String msg,Object obj){
        cleanResult();
        result.setCode(500);
        result.setMsg(msg);
        result.setData(obj);
        return result;
    }


    private static void cleanResult(){
        result.setCode(500);
        result.setMsg(null);
        result.setData(null);
    }
}
