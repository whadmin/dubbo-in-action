/*
 *
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.dubbo.samples.exception;

import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;

/**
 * Dubbo异常处理示例消费者
 * 演示了各种异常场景的处理方式和最佳实践
 */
public class ExceptionConsumer {

    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_RETRY_DELAY_MS = 1000;

    public static void main(String[] args) {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/exception-consumer.xml")) {
            context.start();
            GreetingsService greetingsService = context.getBean("exceptionService", GreetingsService.class);
            
            System.out.println("===== 基本异常处理示例 =====");
            demonstrateBasicExceptionHandling(greetingsService);
            
            System.out.println("\n===== 可恢复异常和重试机制示例 =====");
            demonstrateRetryMechanism(greetingsService);
            
            System.out.println("\n===== 超时处理示例 =====");
            demonstrateTimeoutHandling(greetingsService);
            
            System.out.println("\n===== 错误降级示例 =====");
            demonstrateFallbackMechanism(greetingsService);
        }
    }
    
    /**
     * 演示基本异常处理方式
     */
    private static void demonstrateBasicExceptionHandling(GreetingsService service) {
        // 正常调用
        executeWithExceptionHandling(
            () -> service.sayHi("Dubbo"), 
            "正常调用"
        );
        
        // 业务异常调用
        executeWithExceptionHandling(
            () -> service.sayHi("exception"), 
            "业务异常调用"
        );
        
        // 运行时异常调用
        executeWithExceptionHandling(
            () -> service.sayHi("runtime"), 
            "运行时异常调用"
        );
        
        // 空指针异常调用
        executeWithExceptionHandling(
            () -> service.sayHi("null"), 
            "空指针异常调用"
        );
    }
    
    /**
     * 演示可恢复异常和重试机制
     */
    private static void demonstrateRetryMechanism(GreetingsService service) {
        executeWithRetry(
            () -> service.recoverableMethod("recoverable"),
            MAX_RETRIES,
            "可恢复方法调用"
        );
    }
    
    /**
     * 演示超时处理
     */
    private static void demonstrateTimeoutHandling(GreetingsService service) {
        // 正常超时范围内调用
        executeWithExceptionHandling(
            () -> service.timeoutMethod(500),
            "正常范围超时调用"
        );
        
        // 超出超时限制的调用
        executeWithExceptionHandling(
            () -> service.timeoutMethod(3000),
            "超出超时限制调用",
            "默认超时结果"
        );
    }
    
    /**
     * 演示服务降级机制
     */
    private static void demonstrateFallbackMechanism(GreetingsService service) {
        // 模拟网络故障，触发降级
        String result = executeWithFallback(
            () -> service.sayHi("network_error"),
            "网络错误场景",
            ExceptionConsumer::getNetworkErrorFallback
        );
        System.out.println("降级结果: " + result);
    }
    
    /**
     * 通用异常处理执行方法
     * 
     * @param supplier 要执行的操作
     * @param operationName 操作名称（用于日志）
     * @return 操作结果或null（如果发生异常）
     */
    private static <T> T executeWithExceptionHandling(ThrowingSupplier<T> supplier, String operationName) {
        return executeWithExceptionHandling(supplier, operationName, null);
    }
    
    /**
     * 带默认值的异常处理执行方法
     * 
     * @param supplier 要执行的操作
     * @param operationName 操作名称（用于日志）
     * @param defaultValue 发生异常时的默认返回值
     * @return 操作结果或默认值
     */
    private static <T> T executeWithExceptionHandling(ThrowingSupplier<T> supplier, String operationName, T defaultValue) {
        System.out.println("执行操作: " + operationName);
        
        try {
            T result = supplier.get();
            System.out.println("操作成功: " + result);
            return result;
        } catch (BusinessException e) {
            System.err.println("业务异常: " + e.getMessage() + ", 错误码: " + e.getCode());
            // 业务异常特定处理逻辑可以放在这里
            return defaultValue;
        } catch (RpcException e) {
            handleRpcException(e, operationName);
            return defaultValue;
        } catch (Exception e) {
            System.err.println("未预期的异常: " + e.getClass().getName() + ": " + e.getMessage());
            return defaultValue;
        } finally {
            // 清理上下文，防止信息泄漏到下一次调用
            RpcContext.getServiceContext().clearAttachments();
        }
    }
    
    /**
     * 处理RPC异常的专用方法
     */
    private static void handleRpcException(RpcException e, String operationName) {
        System.err.println("RPC异常 [" + operationName + "]: " + e.getMessage());
        
        if (e.isTimeout()) {
            System.err.println("调用超时，执行降级逻辑");
            handleTimeoutFallback();
        } else if (e.isNetwork()) {
            System.err.println("网络异常，尝试使用备用服务");
            tryBackupService();
        } else if (e.isSerialization()) {
            System.err.println("序列化异常，可能是接口不兼容");
        } else if (e.isForbidden()) {
            System.err.println("禁止访问异常，可能是权限问题");
        } else {
            System.err.println("其他RPC异常，返回默认结果");
        }
    }
    
    /**
     * 带重试机制的执行方法
     * 
     * @param supplier 要执行的操作
     * @param maxRetries 最大重试次数
     * @param operationName 操作名称
     * @return 操作结果或null
     */
    private static <T> T executeWithRetry(ThrowingSupplier<T> supplier, int maxRetries, String operationName) {
        System.out.println("执行带重试的操作: " + operationName);
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                System.out.println("尝试 #" + (attempt + 1) + "/" + maxRetries);
                T result = supplier.get();
                System.out.println("操作成功: " + result);
                return result;
            } catch (BusinessException e) {
                // 业务异常不重试
                System.err.println("业务异常无需重试: " + e.getMessage());
                return null;
            } catch (RpcException e) {
                if (isRetryableException(e)) {
                    if (attempt < maxRetries - 1) {
                        int delayMs = calculateRetryDelay(attempt);
                        System.err.println("发生可重试异常: " + e.getMessage() + 
                                          "，将在" + delayMs + "ms后重试");
                        sleep(delayMs);
                    } else {
                        System.err.println("重试次数用尽，操作失败: " + e.getMessage());
                    }
                } else {
                    System.err.println("不可重试的RPC异常: " + e.getMessage());
                    return null;
                }
            } catch (Exception e) {
                System.err.println("不可重试的其他异常: " + e.getClass().getName() + ": " + e.getMessage());
                return null;
            } finally {
                RpcContext.getServiceContext().clearAttachments();
            }
        }
        return null;
    }
    
    /**
     * 带降级机制的执行方法
     * 
     * @param supplier 要执行的操作
     * @param operationName 操作名称
     * @param fallback 降级逻辑
     * @return 操作结果或降级结果
     */
    private static <T> T executeWithFallback(
            ThrowingSupplier<T> supplier, 
            String operationName,
            Supplier<T> fallback) {
        
        System.out.println("执行带降级的操作: " + operationName);
        
        try {
            T result = supplier.get();
            System.out.println("操作成功: " + result);
            return result;
        } catch (Exception e) {
            System.err.println("操作失败，启用降级: " + e.getMessage());
            return fallback.get();
        } finally {
            RpcContext.getServiceContext().clearAttachments();
        }
    }
    
    /**
     * 判断异常是否可以重试
     */
    private static boolean isRetryableException(RpcException e) {
        return e.isTimeout() || e.isNetwork();
    }
    
    /**
     * 计算重试延迟时间（指数退避策略）
     */
    private static int calculateRetryDelay(int attempt) {
        // 指数退避: 1s, 2s, 4s...
        return INITIAL_RETRY_DELAY_MS * (1 << attempt);
    }
    
    /**
     * 线程休眠帮助方法
     */
    private static void sleep(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 超时降级逻辑
     */
    private static void handleTimeoutFallback() {
        System.out.println("执行本地降级逻辑，返回缓存数据或默认值");
    }
    
    /**
     * 尝试备用服务
     */
    private static void tryBackupService() {
        System.out.println("尝试调用备用服务或使用本地缓存");
    }
    
    /**
     * 获取网络错误的降级响应
     */
    private static String getNetworkErrorFallback() {
        return "这是一个降级响应 (当前时间: " + System.currentTimeMillis() + ")";
    }
    
    /**
     * 可抛出异常的Supplier接口
     */
    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
