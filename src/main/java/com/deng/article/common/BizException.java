package com.deng.article.common;

/**
 * 业务异常：服务层抛这个，全局处理器统一转成 JSON
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(400, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}