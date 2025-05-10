package org.apache.dubbo.samples.timeout;

import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.samples.timeout.api.TimeoutService;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * Dubbo超时机制测试类
 * 
 * 超时配置架构：
 * 
 * 1. 消费者方配置（Consumer端）:
 *    - 消费者级别：<dubbo:consumer timeout="2000"/> (consumer.xml)
 *    - 服务级别：<dubbo:reference timeout="3000"/> (consumer.xml)
 *    - 方法级别：<dubbo:method name="methodLevelTimeout" timeout="4000"/> (consumer.xml)
 * 
 * 2. 生产者方配置（Provider端）:
 *    - 提供者级别：<dubbo:provider timeout="3000"/> (provider.xml)
 *    - 服务级别：<dubbo:service timeout="4000"/> (provider.xml)
 *    - 方法级别：<dubbo:method name="methodLevelTimeout" timeout="5000"/> (provider.xml)
 * 
 * 3. 超时配置优先级:
 *    1) 方法级 > 服务级 > 消费者级/提供者级
 *    2) 消费者端 > 提供者端
 * 
 * 完整优先级排序:
 *    消费者方法级 > 消费者服务级 > 提供者方法级 > 提供者服务级 > 消费者级 > 提供者级
 * 
 * 当前测试环境中实际生效的配置:
 *    - methodLevelTimeout方法: 4000ms (消费者方法级配置生效)
 *    - serviceLevelTimeout方法: 3000ms (消费者服务级配置生效)
 *    - normalCall方法: 3000ms (消费者服务级配置生效)
 */
public class TimeoutServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(TimeoutServiceTest.class);
    private TimeoutService timeoutService;
    
    // 各级别超时配置常量
    private static final int CONSUMER_METHOD_LEVEL_TIMEOUT = 4000; // 消费者方法级超时-methodLevelTimeout
    private static final int PROVIDER_METHOD_LEVEL_TIMEOUT = 5000; // 提供者方法级超时-methodLevelTimeout
    private static final int CONSUMER_SERVICE_LEVEL_TIMEOUT = 3000; // 消费者服务级超时
    private static final int PROVIDER_SERVICE_LEVEL_TIMEOUT = 4000; // 提供者服务级超时
    private static final int CONSUMER_LEVEL_TIMEOUT = 2000; // 消费者默认超时
    private static final int PROVIDER_LEVEL_TIMEOUT = 3000; // 提供者默认超时

    @Before
    public void setUp() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/timeout-consumer.xml");
        context.start();
        timeoutService = context.getBean("timeoutService", TimeoutService.class);
        logger.info("测试环境已初始化，当前有效超时配置: " +
                "methodLevelTimeout={}ms(消费者方法级), " +
                "serviceLevelTimeout={}ms(消费者服务级), " +
                "normalCall={}ms(消费者服务级)",
                CONSUMER_METHOD_LEVEL_TIMEOUT, CONSUMER_SERVICE_LEVEL_TIMEOUT, CONSUMER_SERVICE_LEVEL_TIMEOUT);
    }

    /**
     * 测试正常调用，不会触发超时
     * 
     * 适用超时配置：消费者服务级 3000ms
     */
    @Test
    public void testNormalCall() {
        logger.info("开始测试: 正常调用");
        String result = timeoutService.normalCall();
        assertEquals("正常调用成功", result);
        logger.info("测试通过: 正常调用返回 '{}'", result);
    }

    /**
     * 测试服务级别超时配置 - 不触发超时的情况
     * 
     * 适用超时配置：消费者服务级 3000ms
     * 测试数据：1000ms < 3000ms
     */
    @Test
    public void testServiceLevelTimeout() {
        int delayMs = 1000;
        logger.info("开始测试: 服务级别超时 - 正常情况 (延迟{}ms < 超时{}ms)", 
                delayMs, CONSUMER_SERVICE_LEVEL_TIMEOUT);
        
        String result = timeoutService.serviceLevelTimeout(delayMs);
        assertEquals("延迟 " + delayMs + " 毫秒后调用成功", result);
        logger.info("测试通过: 延迟{}ms调用返回 '{}'", delayMs, result);
    }

    /**
     * 测试服务级别超时配置 - 触发超时的情况
     * 
     * 适用超时配置：消费者服务级 3000ms
     * 测试数据：3500ms > 3000ms
     */
    @Test
    public void testServiceLevelTimeoutLimit() {
        int delayMs = 3500;
        logger.info("开始测试: 服务级别超时 - 超时情况 (延迟{}ms > 超时{}ms)", 
                delayMs, CONSUMER_SERVICE_LEVEL_TIMEOUT);
        
        try {
            timeoutService.serviceLevelTimeout(delayMs);
            fail("应该抛出RpcException");
        } catch (RpcException e) {
            logger.info("预期的超时异常: {}", e.getMessage());
            assertEquals("应该是超时类型的异常", RpcException.TIMEOUT_EXCEPTION, e.getCode());
            assertTrue("异常信息应包含'timeout'或'超时'关键字", 
                    e.getMessage().contains("timeout") || e.getMessage().contains("超时"));
            
            // 超时异常处理 - 降级策略示例
            logger.info("超时异常处理 - 执行降级逻辑");
            // 在实际应用中，这里可以实现降级逻辑，如返回缓存数据或默认值
        }
    }

    /**
     * 测试方法级别的超时配置 - 不触发超时的情况
     * 
     * 适用超时配置：消费者方法级 4000ms
     * 测试数据：3800ms < 4000ms
     */
    @Test
    public void testMethodLevelTimeout() {
        int delayMs = 3800;
        logger.info("开始测试: 方法级别超时 - 正常情况 (延迟{}ms < 消费者方法级超时{}ms)", 
                delayMs, CONSUMER_METHOD_LEVEL_TIMEOUT);
        
        String result = timeoutService.methodLevelTimeout(delayMs);
        assertEquals("方法级别超时设置测试：延迟 " + delayMs + " 毫秒后调用成功", result);
        logger.info("测试通过: 延迟{}ms调用返回 '{}'", delayMs, result);
    }

    /**
     * 测试方法级别的超时配置 - 触发超时的情况
     * 
     * 适用超时配置：消费者方法级 4000ms
     * 测试数据：4500ms > 4000ms
     */
    @Test
    public void testMethodLevelTimeoutLimit() {
        int delayMs = 4500;
        logger.info("开始测试: 方法级别超时 - 超时情况 (延迟{}ms > 消费者方法级超时{}ms)", 
                delayMs, CONSUMER_METHOD_LEVEL_TIMEOUT);
        
        try {
            timeoutService.methodLevelTimeout(delayMs);
            fail("应该抛出RpcException");
        } catch (RpcException e) {
            logger.info("预期的超时异常: {}", e.getMessage());
            assertEquals("应该是超时类型的异常", RpcException.TIMEOUT_EXCEPTION, e.getCode());
            
            // 超时异常处理 - 尝试用更短的延迟重试
            try {
                logger.info("超时异常处理 - 使用更短延迟重试");
                int retryDelayMs = 1000;
                String retryResult = timeoutService.methodLevelTimeout(retryDelayMs);
                logger.info("重试成功: '{}'", retryResult);
            } catch (RpcException retryEx) {
                logger.error("重试也失败: {}", retryEx.getMessage());
                // 最终降级 - 使用不会超时的调用
                String fallback = timeoutService.normalCall();
                logger.info("执行最终降级: '{}'", fallback);
            }
        }
    }
    
    /**
     * 测试异步超时控制 - 使用CompletableFuture
     * 
     * 优势：
     * 1. 客户端可以设置比RPC超时更短的等待时间
     * 2. 在等待时间内快速失败，提高用户体验
     * 3. 可以取消正在执行的任务，避免资源浪费
     */
    @Test
    public void testTimeoutWithCompletableFuture() {
        int delayMs = 6000; // 会导致RPC超时
        int clientTimeoutMs = 2000; // 客户端等待时间比RPC超时更短
        
        logger.info("开始测试: 异步超时控制 (客户端等待{}ms < RPC超时{}ms < 执行时间{}ms)",
                clientTimeoutMs, CONSUMER_METHOD_LEVEL_TIMEOUT, delayMs);
        
        // 创建异步任务
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                return timeoutService.methodLevelTimeout(delayMs);
            } catch (RpcException e) {
                throw new RuntimeException("RPC调用超时: " + e.getMessage(), e);
            }
        });
        logger.info("异步任务创建成功");
        try {
            // 客户端等待时间更短，可以快速响应
            String result = future.get(clientTimeoutMs, TimeUnit.MILLISECONDS);
            fail("应该超时");
        } catch (TimeoutException e) {
            logger.info("客户端等待超时: {}", e.getMessage());
            boolean cancelled = future.cancel(true);
            logger.info("取消后台任务: {}", cancelled ? "成功" : "失败");
            
            // 降级处理
            String fallbackResult = timeoutService.normalCall();
            assertEquals("正常调用成功", fallbackResult);
            logger.info("降级调用成功: '{}'", fallbackResult);
        } catch (Exception e) {
            logger.error("意外异常: {}", e.getMessage(), e);
            fail("出现意外异常: " + e.getMessage());
        }
    }
}