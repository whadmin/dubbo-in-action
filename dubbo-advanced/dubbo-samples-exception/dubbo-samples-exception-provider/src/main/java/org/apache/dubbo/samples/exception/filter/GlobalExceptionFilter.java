package org.apache.dubbo.samples.exception.filter;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.*;

/**
 * 服务提供者全局异常处理过滤器
 * 用于将异常转换成标准业务异常，避免框架细节泄露
 */
@Activate(group = CommonConstants.PROVIDER)
public class GlobalExceptionFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            // 执行原始调用
            Result result = invoker.invoke(invocation);
            
            // 检查结果中是否有异常
            if (result.hasException()) {
                Throwable exception = result.getException();
                
                // 记录异常日志
                logger.error("服务调用过程中发生异常: " + exception.getMessage(), exception);
                
                // 对不同类型异常进行处理
                if (exception instanceof RuntimeException) {
                    // 运行时异常直接抛出
                    return result;
                } else if (exception instanceof OutOfMemoryError 
                        || exception instanceof StackOverflowError) {
                    // 系统严重错误，应记录并返回友好提示
                    return new AppResponse(new RuntimeException("服务器内部错误，请稍后重试"));
                } else {
                    // 其他异常，包装成业务友好的异常
                    return new AppResponse(new RuntimeException("服务处理失败: " + exception.getMessage()));
                }
            }
            return result;
        } catch (Throwable t) {
            // 捕获可能的RPC框架异常
            logger.error("RPC调用过程发生未预期异常", t);
            throw new RpcException("服务暂时不可用，请稍后重试", t);
        }
    }
}