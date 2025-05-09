package org.apache.dubbo.samples.timeout.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TimeoutService 接口实现类
 * 用于演示不同的超时场景
 */
public class TimeoutServiceImpl implements TimeoutService {
    
    private static final Logger logger = LoggerFactory.getLogger(TimeoutServiceImpl.class);
    
    @Override
    public String normalCall() {
        logger.info("执行normalCall方法，正常响应");
        return "正常调用成功";
    }
    
    @Override
    public String timeoutCall(int delayMs) {
        logger.info("执行timeoutCall方法，延迟 {} 毫秒", delayMs);
        try {
            // 模拟方法执行延迟
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "调用被中断";
        }
        return "延迟 " + delayMs + " 毫秒后调用成功";
    }
    
    @Override
    public String methodLevelTimeout(int delayMs) {
        logger.info("执行methodLevelTimeout方法，延迟 {} 毫秒", delayMs);
        try {
            // 模拟方法执行延迟
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "调用被中断";
        }
        return "方法级别超时设置测试：延迟 " + delayMs + " 毫秒后调用成功";
    }
    
    @Override
    public String paramLevelTimeout(int delayMs, int timeoutMs) {
        logger.info("执行paramLevelTimeout方法，延迟 {} 毫秒，超时设置 {} 毫秒", delayMs, timeoutMs);
        try {
            // 模拟方法执行延迟
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "调用被中断";
        }
        return "参数级别超时设置测试：延迟 " + delayMs + " 毫秒后调用成功";
    }
}
