/*
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
 */

package org.apache.dubbo.samples.context;

import org.apache.dubbo.rpc.RpcContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Dubbo上下文传递示例消费者
 * 
 * 本类演示了Dubbo中各种场景下的上下文传递功能，包括：
 * 1. 基础上下文信息获取
 * 2. 链路追踪信息传递（显式和隐式）
 * 3. 异步调用中的上下文传递
 * 4. 级联调用的上下文传递
 * 5. 不同类型的参数传递方式对比
 */
public class ContextConsumer {
    // 添加日志支持，替代直接打印
    private static final Logger logger = LoggerFactory.getLogger(ContextConsumer.class);
    
    // 用于存储客户端本地上下文的Map（模拟ClientContext）
    private static final Map<String, Object> LOCAL_CONTEXT = new HashMap<>();

    /**
     * 主方法：启动消费者并执行各种上下文传递测试
     */
    public static void main(String[] args) {
        // 加载Spring配置，启动Dubbo消费者
        ClassPathXmlApplicationContext context = null;
        
        try {
            // 初始化上下文
            context = new ClassPathXmlApplicationContext("spring/context-consumer.xml");
            context.start();
            logger.info("Dubbo消费者已启动");

            // 获取远程服务代理
            GreetingsService contextService = context.getBean("contextService", GreetingsService.class);

            // 依次执行各种场景测
            testBasicInfo(contextService);
            testTrace(contextService);
            testCascadeCall(contextService);
            testParameterPassing(contextService);
            testAsyncCall(contextService);

            logger.info("所有测试完成!");
        } catch (Exception e) {
            logger.error("测试过程中发生异常", e);
        } finally {
            // 确保上下文正确关闭
            if (context != null) {
                context.close();
                logger.info("Dubbo消费者已关闭");
            }
        }
    }

    /**
     * 测试场景一：获取基本上下文信息
     * 
     * 此测试演示如何通过Dubbo的RpcContext传递基础附件信息，
     * 并从服务提供者获取基本的上下文信息
     * 
     * @param contextService 远程服务引用
     */
    private static void testBasicInfo(GreetingsService contextService) {
        logger.info("======= 测试基础场景：获取基本上下文信息 =======");

        try {
            // 设置要传递给服务提供方的附件（通过RPC调用传递）
            RpcContext.getClientAttachment().setAttachment("clientId", "consumer-" + System.currentTimeMillis());
            RpcContext.getClientAttachment().setAttachment("userId", "user_" + System.currentTimeMillis() % 1000);
            RpcContext.getClientAttachment().setAttachment("clientVersion", "JavaClient/2.0");

            logger.info("发送基础信息请求，客户端ID: {}", 
                    RpcContext.getClientAttachment().getAttachment("clientId"));

            // 调用远程服务
            String result = contextService.getBasicInfo("BasicInfoClient");
            logger.info("基础信息服务响应结果: \n{}", result);

        } catch (Exception e) {
            logger.error("基础信息调用失败", e);
        } finally {
            // 清理本次测试的本地上下文
            LOCAL_CONTEXT.remove("callId");
            LOCAL_CONTEXT.remove("startTime");
        }
    }

    /**
     * 测试场景二：链路追踪信息传递
     * 
     * 此测试演示两种不同的链路追踪信息传递方式：
     * 1. 通过RpcContext隐式传递跟踪信息
     * 2. 通过方法参数显式传递跟踪信息
     * 
     * @param contextService 远程服务引用
     */
    private static void testTrace(GreetingsService contextService) {
        logger.info("======= 测试链路追踪场景 =======");
        try {
            // 1. 通过RpcContext隐式传递跟踪信息
            String traceId = UUID.randomUUID().toString();
            String spanId = "1";
            RpcContext.getClientAttachment().setAttachment("traceId", traceId);
            RpcContext.getClientAttachment().setAttachment("spanId", spanId);
            RpcContext.getClientAttachment().setAttachment("parentSpanId", "0");
            logger.info("发送跟踪请求（隐式传递），TraceId: {}", traceId);
            String implicitResult = contextService.trace("隐式跟踪请求", null);
            logger.info("隐式跟踪结果: \n{}", implicitResult);

            // 2. 通过方法参数显式传递跟踪信息
            Map<String, String> traceHeaders = new HashMap<>();
            traceId = UUID.randomUUID().toString();
            traceHeaders.put("traceId", traceId);
            traceHeaders.put("spanId", "1.0");
            traceHeaders.put("parentSpanId", "0");
            traceHeaders.put("samplingRate", "1.0");
            logger.info("发送跟踪请求（显式传递），TraceId: {}", traceId);
            RpcContext.getClientAttachment().setAttachment("hiddenTraceId", "hidden-" + UUID.randomUUID().toString());
            String explicitResult = contextService.trace("显式跟踪请求", traceHeaders);
            logger.info("显式跟踪结果: \n{}", explicitResult);
        } catch (Exception e) {
            logger.error("跟踪请求失败", e);
        }
    }

    /**
     * 测试场景三：异步调用中的上下文传递
     * 
     * 此测试演示在异步调用中如何传递上下文信息，
     * 以及如何处理异步调用的结果
     * 
     * @param contextService 远程服务引用
     */
    private static void testAsyncCall(GreetingsService contextService) {
        logger.info("======= 测试异步调用场景 =======");

        // 设置传递给服务方的附件
        RpcContext.getClientAttachment().setAttachment("traceId", "ASYNC_" + UUID.randomUUID().toString());
        RpcContext.getClientAttachment().setAttachment("asyncMarker", "true");
        RpcContext.getClientAttachment().setAttachment("timestamp", String.valueOf(System.currentTimeMillis()));

        // 异步调用需要等待结果
        CountDownLatch latch = new CountDownLatch(1);
        try {
            // 直接调用返回CompletableFuture的方法
            CompletableFuture<String> future = contextService.getInfoAsync("异步请求");
            // 添加完成回调
            future.whenComplete((result, exception) -> {
                try {
                    if (exception != null) {
                        logger.error("异步调用异常", exception);
                    } else {
                        logger.info("异步服务响应结果: \n{}", result);
                    }
                } finally {
                    latch.countDown();
                }
            });
            // 等待异步调用完成或超时
            if (!latch.await(10, TimeUnit.SECONDS)) {
                logger.warn("异步调用等待超时");
            }
        } catch (Exception e) {
            logger.error("异步调用发生异常", e);
            latch.countDown();
        }
    }

    /**
     * 测试场景四：级联调用中的上下文传递
     * 
     * 此测试演示在服务之间的级联调用过程中，
     * 如何传递和维护上下文信息
     * 
     * @param contextService 远程服务引用
     */
    private static void testCascadeCall(GreetingsService contextService) {
        logger.info("======= 测试级联调用场景 =======");

        try {
            // 设置级联调用的跟踪ID
            String traceId = "CASCADE_" + UUID.randomUUID().toString();
            RpcContext.getClientAttachment().setAttachment("traceId", traceId);
            RpcContext.getClientAttachment().setAttachment("spanId", "root");
            RpcContext.getClientAttachment().setAttachment("level", "1");

            logger.info("发起级联调用请求，TraceId: {}", traceId);

            // 调用级联服务，第二个参数为true表示需要继续级联调用
            String result = contextService.cascadeCall("级联调用请求", true);

            logger.info("级联调用结果: \n{}", result);

        } catch (Exception e) {
            logger.error("级联调用失败", e);
        }
    }

    /**
     * 测试场景五：不同参数传递方式对比
     * 
     * 此测试演示并对比两种参数传递方式：
     * 1. 方法参数显式传递 - 通过方法参数直接传递
     * 2. 附件隐式传递 - 通过RpcContext传递附件
     * 
     * @param contextService 远程服务引用
     */
    private static void testParameterPassing(GreetingsService contextService) {
        logger.info("======= 测试参数传递方式 =======");

        try {
            // 1. 设置显式传递的上下文参数 - 通过方法参数传递
            Map<String, Object> contextParams = new HashMap<>();
            contextParams.put("explicitParam1", "值1");
            contextParams.put("explicitParam2", 100);
            contextParams.put("explicitParam3", true);

            // 2. 设置隐式传递的上下文参数 - 通过RpcContext传递
            RpcContext.getClientAttachment().setAttachment("implicitParam1", "隐式值1");
            RpcContext.getClientAttachment().setAttachment("implicitParam2", "隐式值2");

            logger.info("发送参数传递请求");
            logger.info("显式参数: {}", contextParams);
            logger.info("隐式参数: implicitParam1=隐式值1, implicitParam2=隐式值2");

            // 调用服务，同时使用显式和隐式参数
            String result = contextService.passParameters("常规值", contextParams);

            logger.info("参数传递测试结果: \n{}", result);

        } catch (Exception e) {
            logger.error("参数传递测试失败", e);
        }
    }
}
