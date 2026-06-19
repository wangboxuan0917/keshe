package com.keshe.common.exception;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** 参数错误 */
    public static BusinessException badRequest(String msg) {
        return new BusinessException(400, msg);
    }

    /** 数据不存在 */
    public static BusinessException notFound(String msg) {
        return new BusinessException(404, msg);
    }

    /** 冲突/重复 */
    public static BusinessException conflict(String msg) {
        return new BusinessException(409, msg);
    }
}
