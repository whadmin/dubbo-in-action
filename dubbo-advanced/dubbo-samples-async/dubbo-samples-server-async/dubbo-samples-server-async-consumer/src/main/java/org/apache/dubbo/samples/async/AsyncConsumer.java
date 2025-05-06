package org.apache.dubbo.samples.async;

import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcContextAttachment;
import org.apache.dubbo.samples.async.GreetingsService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Dubbo 服务端异步调用消费者示例
 * 
 * 该示例演示了如何调用 Dubbo 服务端的两种异步实现方式：
 * 1. 通过 AsyncContext 实现的异步方法 (sayHiAsync)
 * 2. 通过 CompletableFuture 返回值实现的异步方法 (sayHiFuture)
 * 
 * 关键点：
 * - 对于服务端异步，消费端的调用方式与同步方法基本相同
 * - 可以通过 RpcContext 来传递附件信息
 * - 服务端异步对消费者是透明的，消费者无需感知服务端的异步实现方式
 */
public class AsyncConsumer {

    public static void main(String[] args) throws Exception {
        // 加载 Spring 配置文件，初始化容器
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/async-consumer.xml");
        context.start();

        // 获取异步服务的引用
        GreetingsService asyncService = context.getBean("asyncService", GreetingsService.class);

        // 调用方式一：AsyncContext 异步方式
        callAsyncContextMethod(asyncService);
        
        // 调用方式二：CompletableFuture 异步方式
        callCompletableFutureMethod(asyncService);

        // 等待一段时间以便查看结果
        Thread.sleep(2000);
        
        // 关闭 Spring 容器
        context.close();
    }

    /**
     * 调用基于 AsyncContext 实现的服务端异步方法
     * 
     * 特点：
     * - 接口与同步方法相同，返回类型为普通类型
     * - 消费者无需感知服务端的异步实现
     * - 对于消费者来说，调用方式与同步调用完全一致
     */
    private static void callAsyncContextMethod(GreetingsService service) {
        System.out.println("=== 调用 AsyncContext 方式实现的异步方法 ===");
        
        // 设置附件信息，将传递给服务提供者
        RpcContext.getContext().setAttachment("consumer-key1", "consumer-value1");
        RpcContext.getContext().setAttachment("filters", "async-context-filter");
        
        // 调用服务端异步方法 - 消费者以同步方式调用
        String result = service.sayHiAsync("async context call");
        System.out.println("AsyncContext 方式调用结果: " + result);
        System.out.println("消费者收到服务端附件: " + RpcContext.getContext().getAttachment("server-key1"));
    }
    
    /**
     * 调用基于 CompletableFuture 实现的服务端异步方法
     * 
     * 特点：
     * - 接口返回类型为 CompletableFuture
     * - 消费者可以选择同步等待结果或异步处理
     * - 支持更丰富的异步编程模型，如任务编排、异常处理等
     */
    private static void callCompletableFutureMethod(GreetingsService service) {
        System.out.println("\n=== 调用 CompletableFuture 方式实现的异步方法 ===");
        
        // 设置附件信息
        RpcContext.getContext().setAttachment("consumer-key1", "future-value1");
        RpcContext.getContext().setAttachment("filters", "completable-future-filter");
        
        try {
            // 调用返回 CompletableFuture 的方法
            CompletableFuture<String> future = service.sayHiFuture("completable future call");
            
            // 方式一：同步等待结果（设置超时时间）
            String result = future.get(12, TimeUnit.SECONDS);
            System.out.println("CompletableFuture 方式同步调用结果: " + result);
            
            // 从 RpcContext 获取服务端返回的附件
            System.out.println("消费者收到服务端附件: " + RpcContext.getContext().getAttachment("server-key1"));
            
            // 方式二：异步处理结果（不阻塞当前线程）
            service.sayHiFuture("another future call")
                .thenAccept(response -> {
                    System.out.println("CompletableFuture 方式异步回调结果: " + response);
                })
                .exceptionally(ex -> {
                    System.err.println("调用异常: " + ex.getMessage());
                    return null;
                });
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.err.println("调用异常: " + e.getMessage());
        }
    }
}