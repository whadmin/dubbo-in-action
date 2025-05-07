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

package org.apache.dubbo.samples.validation;

import org.apache.dubbo.rpc.RpcContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ValidationConsumer {

    public static void main(String[] args) throws Exception {
        // 加载Spring配置文件，启动Spring容器
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/validation-consumer.xml");
        context.start();

        // 获取远程服务代理
        RpcContext.getClientAttachment().setAttachment("consumer-key1", "consumer-value1");
        RpcContext.getServiceContext().setAttachment("consumer-key2", "consumer-value2");
        GreetingsService validationService = context.getBean("validationService", GreetingsService.class);
        
        // 调用远程方法
        String world = validationService.sayHi("world");

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
