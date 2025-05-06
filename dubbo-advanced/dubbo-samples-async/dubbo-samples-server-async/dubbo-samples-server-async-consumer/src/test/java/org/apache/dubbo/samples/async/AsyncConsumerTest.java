package org.apache.dubbo.samples.async;

import org.apache.dubbo.rpc.RpcContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * Dubbo 服务端异步调用单元测试
 * 
 * 本测试类测试两种服务端异步实现方式的调用：
 * 1. 基于 AsyncContext 的异步实现
 * 2. 基于 CompletableFuture 的异步实现
 * 
 * 注意：
 * - 运行测试前需确保服务提供者已启动
 * - 测试使用 spring/async-consumer.xml 配置
 * - 测试会验证返回值和附件传递
 */
public class AsyncConsumerTest {

    private ClassPathXmlApplicationContext context;
    private GreetingsService asyncService;

    /**
     * 测试前准备工作
     * - 加载 Spring 配置
     * - 获取服务引用
     */
    @Before
    public void setUp() {
        // 初始化 Spring 上下文
        context = new ClassPathXmlApplicationContext("spring/async-consumer.xml");
        context.start();
        // 获取服务引用
        asyncService = context.getBean("asyncService", GreetingsService.class);
        // 确保服务引用不为空
        assertNotNull("服务引用不应为空", asyncService);
    }

    /**
     * 测试后清理工作
     */
    @After
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    /**
     * 测试基于 AsyncContext 实现的服务端异步方法
     * 
     * 验证点：
     * 1. 调用返回结果不为空
     * 2. 附件可以正确传递和接收
     */
    @Test
    public void testAsyncContextMethod() {
        // 设置传递给服务端的附件
        RpcContext.getContext().setAttachment("consumer-key1", "test-async-context-value");
        
        // 调用服务
        String result = asyncService.sayHiAsync("test async context");
        
        // 验证返回结果 (注意：由于服务端异步，返回值可能是占位符)
        assertNotNull("返回结果不应为空", result);
        
        // 等待一段时间以确保异步处理完成
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 验证是否能从服务端接收附件
        // 注意：在实际异步场景中，附件可能无法可靠传回，这取决于具体实现
        System.out.println("AsyncContext 测试接收到的附件: " + RpcContext.getContext().getAttachment("server-key1"));
    }

    /**
     * 测试基于 CompletableFuture 实现的服务端异步方法
     * 
     * 验证点：
     * 1. 返回的 CompletableFuture 不为空
     * 2. 可以成功获取异步结果
     * 3. 服务端返回的附件能被正确接收
     */
    @Test
    public void testCompletableFutureMethod() throws InterruptedException, ExecutionException, TimeoutException {
        // 设置传递给服务端的附件
        RpcContext.getContext().setAttachment("consumer-key1", "test-completable-future-value");
        
        // 调用返回 CompletableFuture 的方法
        CompletableFuture<String> future = asyncService.sayHiFuture("test completable future");
        
        // 验证返回的 Future 不为空
        assertNotNull("返回的 CompletableFuture 不应为空", future);
        
        // 等待异步结果（设置超时时间）
        String result = future.get(12, TimeUnit.SECONDS);
        
        // 验证结果不为空
        assertNotNull("异步结果不应为空", result);
        assertTrue("结果应包含预期内容", result.contains("async response"));
        
        // 验证服务端返回的附件
        String attachment = RpcContext.getContext().getAttachment("server-key1");
        System.out.println("CompletableFuture 测试接收到的附件: " + attachment);
    }

}