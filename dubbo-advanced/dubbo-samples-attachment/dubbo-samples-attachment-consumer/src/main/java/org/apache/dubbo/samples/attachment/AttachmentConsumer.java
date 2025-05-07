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

package org.apache.dubbo.samples.attachment;

import org.apache.dubbo.rpc.RpcContext;
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
public class AttachmentConsumer {

    public static void main(String[] args) throws Exception {
        // 加载Spring配置文件，启动Spring容器
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/attachment-consumer.xml");
        context.start();

        // 获取远程服务代理
        RpcContext.getClientAttachment().setAttachment("consumer-key1", "consumer-value1");
        GreetingsService attachmentService = context.getBean("attachmentService", GreetingsService.class);
        
        // 演示方式1：通过RpcContext获取Future
        // 调用远程方法 - 由于在xml中配置了async="true"，此调用会立即返回null
        String world = attachmentService.sayHi("world");
        System.out.println("消费者收到服务端附件: " + RpcContext.getServerAttachment().getAttachment("server-key1"));
        context.close();
    }
}
