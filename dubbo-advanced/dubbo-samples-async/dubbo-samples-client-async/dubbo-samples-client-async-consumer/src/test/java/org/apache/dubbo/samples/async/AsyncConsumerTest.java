package org.apache.dubbo.samples.async;

import org.apache.dubbo.rpc.RpcContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Dubbo客户端异步调用测试类
 * 
 * 分别测试三种异步调用方式：
 * 1. 通过RpcContext.getContext().getCompletableFuture()获取Future
 * 2. 通过RpcContext.getContext().asyncCall()进行异步调用
 * 3. 通过接口直接返回CompletableFuture
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring/async-consumer.xml"})
public class AsyncConsumerTest {

    @Autowired
    private GreetingsService greetingsService;

    private CountDownLatch latch;

    @Before
    public void setUp() {
        // 每个测试开始前重置计数器
        latch = new CountDownLatch(1);
    }

    @After
    public void tearDown() {
        // 测试结束后等待一些时间，让异步回调有机会执行
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 测试方式1：通过RpcContext.getContext().getCompletableFuture()获取Future
     * 
     * 这种方式需要在服务消费方的XML配置中设置async="true"
     */
    @Test
    public void testAsyncCallWithRpcContextFuture() throws Exception {
        // 调用远程方法，此调用立即返回null
        String result = greetingsService.sayHi("world");
        System.out.println("方式1测试 - 同步调用返回：" + result);
        
        // 从RpcContext获取Future对象
        CompletableFuture<String> future = RpcContext.getContext().getCompletableFuture();
        assertNotNull("Future对象不应为null", future);
        
        // 添加回调
        future.whenComplete((retValue, exception) -> {
            if (exception == null) {
                System.out.println("方式1测试 - 异步调用结果：" + retValue);
                assertTrue("返回值应包含hello", retValue.contains("hello"));
                latch.countDown();
            } else {
                exception.printStackTrace();
            }
        });
        
        // 等待回调执行
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue("异步调用应在5秒内完成", completed);
    }

    /**
     * 测试方式2：通过RpcContext.getContext().asyncCall()进行异步调用
     * 
     * 此方式不依赖于XML中的async配置
     */
    @Test
    public void testAsyncCallWithAsyncCall() throws Exception {
        // 使用asyncCall进行异步调用
        CompletableFuture<String> future = RpcContext.getContext().asyncCall(
                () -> greetingsService.sayHiAsync("asyncCall")
        );
        
        assertNotNull("Future对象不应为null", future);
        
        // 添加回调
        future.whenComplete((retValue, exception) -> {
            if (exception == null) {
                System.out.println("方式2测试 - 异步调用结果：" + retValue);
                assertTrue("返回值应包含hello", retValue.contains("hello"));
                latch.countDown();
            } else {
                exception.printStackTrace();
            }
        });
        
        // 等待回调执行
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue("异步调用应在5秒内完成", completed);
    }
    
    /**
     * 测试方式3：通过接口方法直接返回CompletableFuture
     * 
     * 此方式要求服务接口中有返回CompletableFuture的方法
     */
    @Test
    public void testAsyncCallWithReturnFuture() throws Exception {
        // 调用返回CompletableFuture的接口方法
        CompletableFuture<String> future = greetingsService.sayHiFuture("future");
        
        assertNotNull("Future对象不应为null", future);
        
        // 添加回调
        future.whenComplete((retValue, exception) -> {
            if (exception == null) {
                System.out.println("方式3测试 - 异步调用结果：" + retValue);
                assertTrue("返回值应包含hello", retValue.contains("hello"));
                latch.countDown();
            } else {
                exception.printStackTrace();
            }
        });
        
        // 等待回调执行
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue("异步调用应在5秒内完成", completed);
    }
}