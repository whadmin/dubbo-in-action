package org.apache.dubbo.samples.exception;

/**
 * 业务异常基类
 * 用于区分业务逻辑异常和系统技术异常
 */
public class BusinessException extends Exception {
    private static final long serialVersionUID = 1L;
    
    // 业务错误码
    private String code;
    
    public BusinessException() {
        super();
    }
    
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
}