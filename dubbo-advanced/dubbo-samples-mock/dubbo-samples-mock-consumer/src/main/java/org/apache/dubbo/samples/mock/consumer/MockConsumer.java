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

package org.apache.dubbo.samples.mock.consumer;

import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.samples.mock.GreetingsService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Dubbo客户端异步调用示例
 * <p>
 * Dubbo提供了三种异步调用方式：
 * 1. 使用RpcContext.getContext().getCompletableFuture()获取Future
 * 2. 使用RpcContext.getContext().asyncCall()方法进行异步调用
 * 3. 服务接口中直接返回CompletableFuture
 * </p>
 */
public class MockConsumer {

    public static void main(String[] args) throws Exception {
        // 加载Spring配置文件，启动Spring容器
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/mock-consumer.xml");
        context.start();

        // 获取远程服务代理
        RpcContext.getClientAttachment().setAttachment("consumer-key1", "consumer-value1");
        RpcContext.getServiceContext().setAttachment("consumer-key2", "consumer-value2");
        GreetingsService attachmentService = context.getBean("attachmentService", GreetingsService.class);
        
        // 调用远程方法
        String world = attachmentService.sayHi("world");

        // 尝试不同的上下文获取附件
        System.out.println("consumer-key1 from attachment: " + RpcContext.getClientResponseContext().getAttachment("server-key1"));
        System.out.println("consumer-key1 from attachment: " + RpcContext.getServiceContext().getAttachment("server-key1"));
        System.out.println("consumer-key1 from attachment: " + RpcContext.getClientAttachment().getAttachment("server-key1"));

        System.out.println("consumer-key2 from attachment: " + RpcContext.getClientResponseContext().getAttachment("server-key2"));
        System.out.println("consumer-key2 from attachment: " + RpcContext.getServiceContext().getAttachment("server-key2"));
        System.out.println("consumer-key2 from attachment: " + RpcContext.getClientAttachment().getAttachment("server-key2"));
        context.close();
    }
}
