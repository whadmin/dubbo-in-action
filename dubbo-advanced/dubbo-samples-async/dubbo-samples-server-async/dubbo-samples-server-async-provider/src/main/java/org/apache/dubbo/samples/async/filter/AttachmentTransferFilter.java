package org.apache.dubbo.samples.async.filter;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Dubbo 3.2.6 兼容的附件传输过滤器
 * 用于解决服务端异步调用中附件无法正确传递到消费端的问题
 */
@Activate(group = {CommonConstants.PROVIDER})
public class AttachmentTransferFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(AttachmentTransferFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        logger.info("AttachmentTransferFilter: 开始处理调用 {}", invocation.getMethodName());
        
        // 记录线程ID，用于调试
        final long threadId = Thread.currentThread().getId();
        logger.debug("当前线程ID: {}", threadId);
        
        // 调用服务
        Result result = invoker.invoke(invocation);
        
        // 处理异步结果
        if (result.getValue() instanceof CompletableFuture) {
            logger.info("检测到异步调用，应用异步附件处理");
            
            @SuppressWarnings("unchecked")
            CompletableFuture<Object> future = (CompletableFuture<Object>) result.getValue();
            
            // 处理异步结果完成时的附件传递
            CompletableFuture<Object> newFuture = future.whenComplete((value, exception) -> {
                try {
                    // 从服务上下文中获取附件
                    Map<String, Object> attachments = RpcContext.getServerContext().getObjectAttachments();
                    if (attachments != null && !attachments.isEmpty()) {
                        logger.info("找到服务端附件: {}", attachments.keySet());
                        
                        // 将附件添加到结果中
                        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
                            String key = entry.getKey();
                            Object val = entry.getValue();
                            
                            // 使用 AppResponse 特定的方法添加附件
                            if (result instanceof AppResponse) {
                                ((AppResponse) result).setAttachment(key, val);
                                logger.info("添加异步附件到 AppResponse: {}={}", key, val);
                            } else {
                                // 降级使用通用方法
                                result.setObjectAttachment(key, val);
                                logger.info("添加异步附件到一般结果: {}={}", key, val);
                            }
                        }
                    } else {
                        logger.warn("没有找到服务端附件，这可能是由于异步上下文问题");
                    }
                } catch (Exception e) {
                    logger.error("处理异步附件时发生异常", e);
                }
            });
            
            // 创建新的异步结果
            return AsyncRpcResult.newDefaultAsyncResult(newFuture, invocation);
        } else {
            // 处理同步结果
            logger.info("处理同步结果的附件传递");
            try {
                // 从服务上下文中获取附件
                Map<String, Object> attachments = RpcContext.getServerContext().getObjectAttachments();
                if (attachments != null && !attachments.isEmpty()) {
                    for (Map.Entry<String, Object> entry : attachments.entrySet()) {
                        String key = entry.getKey();
                        Object val = entry.getValue();
                        
                        // 使用 3.2.6 版本兼容的方式设置附件
                        if (result instanceof AppResponse) {
                            ((AppResponse) result).setAttachment(key, val);
                            logger.info("添加同步附件到 AppResponse: {}={}", key, val);
                        } else {
                            result.setObjectAttachment(key, val);
                            logger.info("添加同步附件到一般结果: {}={}", key, val);
                        }
                    }
                } else {
                    logger.warn("同步调用中没有找到附件");
                }
            } catch (Exception e) {
                logger.error("处理同步附件时发生异常", e);
            }
        }
        
        return result;
    }
}
