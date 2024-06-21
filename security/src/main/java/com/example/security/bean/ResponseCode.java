package com.example.security.bean;

public enum ResponseCode {
    SUCCESS(0, "请求成功"),
    HTTP_ERROR(100000, "后台请求异常"),
    HTTP_METHOD_ERROR(100001, "不支持该HTTP请求"),
    NO_LOGIN(100002, "未登录"),
    AUTH_LOGIN_ERROR(100003, "用户名密码错误"),
    AUTH_ERROR(200000, "权限异常"),
    PARAMS_ERROR(200001, "参数为空验证失败"),
    DATA_NOTFOUND(200002, "数据不存在"),
    ROUTE_SAVE_ERROR(300000, "路由保存失败"),
    ROUTE_DELETE_ERROR(300001, "路由删除失败"),
    ROUTE_NOTFOUND_ERROR(300002, "路由不存在");
    private int code;
    private String message;

    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
