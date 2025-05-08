package org.apache.dubbo.samples.attachment.impl;

import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.samples.attachment.GreetingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 问候服务接口的实现类
 * 
 * 这个类演示了如何在服务提供者端处理和管理附件(Attachment)
 */
public class GreetingImpl implements GreetingsService {
    
    private static final Logger logger = LoggerFactory.getLogger(GreetingImpl.class);
    
    @Override
    public String sayHi(String name) {
        // 1. 获取请求附件 - 使用推荐API
        readRequestAttachments();
        
        // 2. 设置响应附件 - 使用推荐API
        setResponseAttachments();
        
        // 3. 返回业务响应
        return "hello, " + name;
    }
    
    /**
     * 获取并记录请求附件信息
     */
    private void readRequestAttachments() {
        // 仅使用推荐的API获取附件
        String value1 = RpcContext.getServerAttachment().getAttachment("consumer-key1");
        String value2 = RpcContext.getServerAttachment().getAttachment("consumer-key2");
        
        // 使用日志框架替代直接打印到控制台
        logger.info("收到请求附件: consumer-key1={}, consumer-key2={}", value1, value2);
        
        // 如果需要在开发阶段验证旧API和新API的兼容性，可以保留以下代码
        if (logger.isDebugEnabled()) {
            String oldValue1 = RpcContext.getServiceContext().getAttachment("consumer-key1");
            String oldValue2 = RpcContext.getServiceContext().getAttachment("consumer-key2");
            logger.debug("通过旧API获取附件: consumer-key1={}, consumer-key2={}", oldValue1, oldValue2);
        }
    }
    
    /**
     * 设置响应附件信息
     */
    private void setResponseAttachments() {
        // 仅使用推荐的API设置响应附件
        RpcContext.getServerResponseContext().setAttachment("server-key1", "server-value1");
        RpcContext.getServerResponseContext().setAttachment("server-key2", "server-value2");
        
        logger.info("已设置响应附件: server-key1=server-value1, server-key2=server-value2");
    }
}
