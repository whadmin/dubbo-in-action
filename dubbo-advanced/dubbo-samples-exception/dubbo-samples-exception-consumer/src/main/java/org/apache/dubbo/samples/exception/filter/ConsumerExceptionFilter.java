package org.apache.dubbo.samples.exception.filter;


import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.samples.exception.BusinessException;

/**
 * 消费端全局异常处理过滤器
 * 用于统一处理来自服务端的异常
 */
@Activate(group = CommonConstants.CONSUMER)
public class ConsumerExceptionFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerExceptionFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            Result result = invoker.invoke(invocation);
            if (result.hasException()) {
                Throwable exception = result.getException();
                
                // 记录异常日志
                logger.error("调用服务发生异常: " + exception.getMessage(), exception);
                
                // 根据异常类型进行处理
                if (exception instanceof BusinessException) {
                    // 业务异常直接抛出，由业务代码处理
                    return result;
                } else if (exception instanceof RpcException) {
                    // RPC异常，可能是网络问题、超时等
                    RpcException rpcException = (RpcException) exception;
                    if (rpcException.isTimeout()) {
                        // 超时异常处理
                        logger.warn("服务调用超时: " + invocation.getMethodName());
                    } else if (rpcException.isNetwork()) {
                        // 网络异常处理
                        logger.warn("网络异常: " + rpcException.getMessage());
                    } else if (rpcException.isBiz()) {
                        // 业务异常处理
                        logger.warn("业务异常: " + rpcException.getMessage());
                    }
                }
            }
            return result;
        } catch (RpcException e) {
            // 捕获框架可能抛出的RpcException
            logger.error("RPC调用异常", e);
            throw e;
        }
    }
}
