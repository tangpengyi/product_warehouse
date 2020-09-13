package com.tpy.product_warehouse.api;

import lombok.Data;

@Data
public class ResponseResult<T> {

    private int code;

    private String msg;

    private T data;

}
