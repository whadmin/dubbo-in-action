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

package org.apache.dubbo.samples.async.impl;


import org.apache.dubbo.rpc.AsyncContext;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcContextAttachment;
import org.apache.dubbo.samples.async.GreetingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Dubbo3 服务端异步实现示例
 * <p>
 * Dubbo 提供了两种服务端异步实现方式：
 * 1. 使用 AsyncContext 实现异步执行（传统模式）
 * 2. 使用 CompletableFuture 返回值实现异步（推荐模式）
 * <p>
 * 服务端异步的主要优势：
 * - 将服务执行与 Dubbo 线程解耦，避免 Dubbo 线程池被长时间占用
 * - 提高系统吞吐量和并发处理能力
 * - 适用于耗时操作（如I/O密集型任务、第三方系统调用等）
 */
public class GreetingImpl implements GreetingsService {

    private static Logger logger = LoggerFactory.getLogger(GreetingImpl.class);

    /**
     * 方式一：使用 AsyncContext 实现服务端异步
     * <p>
     * 实现原理：
     * 1. 通过 RpcContext.startAsync() 获取异步上下文，并告知 Dubbo 当前是异步执行模式
     * 2. 创建新线程执行实际业务逻辑，同时当前线程立即返回
     * 3. 在新线程中调用 asyncContext.signalContextSwitch() 实现上下文切换
     * 4. 使用 asyncContext.write() 写回真正的返回值
     * <p>
     * 配置方式：
     * <dubbo:service interface="org.apache.dubbo.samples.async.GreetingsService" ref="greetingsService"/>
     * 无需额外配置，方法内部处理即可
     */
    @Override
    public String sayHiAsync(String name) {
        // 启动异步上下文，告知 Dubbo 当前方法异步执行
        AsyncContext asyncContext = RpcContext.startAsync();
        logger.info("sayHello start");

        // 创建新线程执行实际业务逻辑
        new Thread(() -> {
            // 切换上下文到当前线程，确保 RpcContext 在新线程中可用
            asyncContext.signalContextSwitch();
            // 从客户端获取附件
            RpcContextAttachment attachmentFromClient = RpcContext.getServerAttachment();
            // 用于向客户端发送附件
            RpcContextAttachment attachmentToClient = RpcContext.getClientAttachment();
            // 读取客户端发送的附件值
            String received = (String) attachmentFromClient.getAttachment("consumer-key1");
            logger.info("consumer-key1 from attachment: " + received);
            // 设置返回给客户端的附件
            attachmentToClient.setAttachment("server-key1", "server-" + received);
            try {
                // 模拟耗时操作
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 写回真正的结果，此结果会被发送给消费端
            asyncContext.write("Hello " + name + ", "
                    + "response from provider.");
        }).start();

        // 主线程立即返回，返回值会被忽略，真正的返回值由 asyncContext.write() 提供
        logger.info("sayHello end");
        return "hello, " + name;  // 此返回值仅作为占位符，不会发送给客户端
    }

    /**
     * 方式二：通过 CompletableFuture 返回值实现服务端异步（推荐方式）
     * <p>
     * 实现原理：
     * 1. 方法直接返回 CompletableFuture 对象，Dubbo 自动识别为异步执行
     * 2. 使用 CompletableFuture.supplyAsync() 在线程池中异步执行任务
     * 3. 同样需要通过 asyncContext.signalContextSwitch() 进行上下文切换
     * 4. 任务完成后，CompletableFuture 的结果将作为响应返回给消费端
     * <p>
     * 配置方式：
     * <dubbo:service interface="org.apache.dubbo.samples.async.GreetingsService" ref="greetingsService"/>
     * 接口方法需要定义返回值为 CompletableFuture<T>
     * <p>
     * 优势：
     * - 代码更简洁，符合 Java 8+ 异步编程风格
     * - 可以方便地进行任务编排和异常处理
     * - 更好的线程池管理（可使用自定义线程池替代默认 ForkJoinPool）
     */
    @Override
    public CompletableFuture<String> sayHiFuture(String name) {
        // 启动异步上下文
        AsyncContext asyncContext = RpcContext.startAsync();
        // 返回 CompletableFuture 对象，Dubbo 将等待其完成并发送结果
        return CompletableFuture.supplyAsync(() -> {
            // 切换上下文到当前线程
            asyncContext.signalContextSwitch();
            // 从客户端获取附件
            RpcContextAttachment attachmentFromClient = RpcContext.getServerAttachment();
            // 用于向客户端发送附件
            RpcContextAttachment attachmentToClient = RpcContext.getServerContext();
            // 读取客户端发送的附件值
            String received = (String) attachmentFromClient.getAttachment("consumer-key1");
            logger.info("consumer-key1 from attachment: " + received);
            received = (String) attachmentFromClient.getAttachment("filters");
            logger.info("filters from attachment: " + received);
            attachmentToClient.setAttachment("filters", received);
            // 设置返回给客户端的附件
            attachmentToClient.setAttachment("server-key1", "server-" + received);
            try {
                // 模拟耗时操作
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 返回最终结果
            return "Hello " + name + ", "
                    + "response from provider.";
        });
    }

    /**
     * 方式三（补充）：通过 @Async 注解实现服务端异步
     *
     * 示例代码:
     * @Async
     * public String sayHiAnnotation(String name) {
     *     try {
     *         Thread.sleep(5000); // 模拟耗时操作
     *     } catch (InterruptedException e) {
     *         e.printStackTrace();
     *     }
     *     return "Async annotation response: " + name;
     * }
     *
     * 配置方式：
     * 1. 在方法上添加 @Async 注解
     * 2. 在 XML 配置中启用注解：<dubbo:annotation package="org.apache.dubbo.samples.async"/>
     * 3. 或在配置类上使用 @EnableDubbo 和 @EnableAsync
     *
     * 实现原理：
     * - Dubbo 通过 AOP 拦截带有 @Async 注解的方法
     * - 将方法执行放入线程池异步处理
     * - 适合简单的异步场景，不需要复杂的上下文处理
     */

    /**
     * 方式四（补充）：基于事件驱动的异步实现
     *
     * 示例代码:
     * public void sayHiEvent(String name) {
     *     // 发布事件到事件总线
     *     eventBus.post(new GreetingEvent(name));
     *     // 立即返回
     * }
     *
     * // 事件监听器在单独的线程中处理
     * @Subscribe
     * public void handleGreeting(GreetingEvent event) {
     *     // 异步处理逻辑
     * }
     *
     * 配置方式：
     * - 集成事件驱动框架（如 Guava EventBus 或 Spring Events）
     * - 配置事件监听器和处理线程池
     *
     * 实现原理：
     * - 将同步调用转换为事件发布
     * - 事件监听器在单独线程中异步处理业务逻辑
     * - 适合于系统内部的解耦和异步处理
     */
}
