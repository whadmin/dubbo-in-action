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

package org.apache.dubbo.samples.async;

import org.apache.dubbo.rpc.RpcContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.CompletableFuture;

/**
 * Dubbo客户端异步调用示例
 * <p>
 * Dubbo提供了三种异步调用方式：
 * 1. 使用RpcContext.getContext().getCompletableFuture()获取Future
 * 2. 使用RpcContext.getContext().asyncCall()方法进行异步调用
 * 3. 服务接口中直接返回CompletableFuture
 * </p>
 */
public class AsyncConsumer {

    public static void main(String[] args) throws Exception {
        // 加载Spring配置文件，启动Spring容器
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/async-consumer.xml");
        context.start();

        // 获取远程服务代理
        RpcContext.getClientAttachment().setAttachment("consumer-key1", "consumer-value1");
        GreetingsService asyncService = context.getBean("asyncService", GreetingsService.class);
        
        // 演示方式1：通过RpcContext获取Future
        // 调用远程方法 - 由于在xml中配置了async="true"，此调用会立即返回null
        String world = asyncService.sayHi("world");
        System.out.println("方式1 - 调用返回值: " + world);
        
        // 从RpcContext获取Future对象
        CompletableFuture<String> helloFuture = RpcContext.getContext().getCompletableFuture();
        // 为Future添加回调函数，处理调用结果
        helloFuture.whenComplete((retValue, exception) -> {
            if (exception == null) {
                System.out.println("方式1 - 异步返回结果: " + retValue);
            } else {
                System.out.println("方式1 - 调用出现异常");
                exception.printStackTrace();
            }
        });

        // 演示方式2：使用RpcContext.asyncCall进行异步调用
        RpcContext.getClientAttachment().setAttachment("consumer-key1", "consumer-value1");
        CompletableFuture<String> f = RpcContext.getContext().asyncCall(() -> asyncService.sayHiAsync("async call request"));
        System.out.println("方式2 - 异步调用结果: " + f.get());
        
        // 演示方式3：接口方法直接返回CompletableFuture
        System.out.println("方式3 - 开始调用返回CompletableFuture的接口方法");
        RpcContext.getClientAttachment().setAttachment("consumer-key1", "consumer-value1");
        CompletableFuture<String> future = asyncService.sayHiFuture("future method");
        
        // 方式3不需要配置async="true"，也不需要从RpcContext获取Future
        future.whenComplete((retValue, exception) -> {
            if (exception == null) {
                System.out.println("方式3 - 异步返回结果: " + retValue);
            } else {
                System.out.println("方式3 - 调用出现异常");
                exception.printStackTrace();
            }
        });
        System.out.println("测试完成，退出应用");
        context.close();
    }
}
