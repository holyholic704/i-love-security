package com.example.common.bean;

import lombok.Data;

@Data
public class ResponseResult {

    private int code;
    private String message;
    private Object data;

    public ResponseResult(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public ResponseResult(ResponseCode response) {
        this.code = response.getCode();
        this.message = response.getMessage();
    }

    public ResponseResult(ResponseCode response, Object data) {
        this.code = response.getCode();
        this.message = response.getMessage();
        this.data = data;
    }

    public static ResponseResult success() {
        return new ResponseResult(ResponseCode.SUCCESS);
    }

    public static <T> ResponseResult success(T data) {
        return new ResponseResult(ResponseCode.SUCCESS, data);
    }

    public static ResponseResult error() {
        return new ResponseResult(ResponseCode.HTTP_ERROR);
    }

    public static ResponseResult error(String message) {
        return new ResponseResult(ResponseCode.HTTP_ERROR.getCode(), message);
    }

    public static ResponseResult code(ResponseCode response) {
        return new ResponseResult(response.getCode(), response.getMessage());
    }
}
