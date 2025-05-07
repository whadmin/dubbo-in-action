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

package org.apache.dubbo.samples.direct.impl;


import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.samples.direct.GreetingsService;

public class GreetingImpl implements GreetingsService {
    @Override
    public String sayHi(String name) {
        // 读取客户端发送的附件值
        System.out.println("consumer-key1 from attachment: " + (String) RpcContext.getServerAttachment().getAttachment("consumer-key1"));
        System.out.println("consumer-key1 from attachment: " + (String) RpcContext.getServiceContext().getAttachment("consumer-key1"));
        System.out.println("consumer-key1 from attachment: " + (String) RpcContext.getServerContext().getAttachment("consumer-key1"));
        System.out.println("consumer-key2 from attachment: " + (String) RpcContext.getServerAttachment().getAttachment("consumer-key2"));
        System.out.println("consumer-key2 from attachment: " + (String) RpcContext.getServiceContext().getAttachment("consumer-key2"));
        System.out.println("consumer-key2 from attachment: " + (String) RpcContext.getServerContext().getAttachment("consumer-key2"));

        RpcContext.getClientResponseContext().setAttachment("server-key1", "server-value1");
        RpcContext.getServerContext().setAttachment("server-key2", "server-value2");
        return "hello, " + name;
    }
}
