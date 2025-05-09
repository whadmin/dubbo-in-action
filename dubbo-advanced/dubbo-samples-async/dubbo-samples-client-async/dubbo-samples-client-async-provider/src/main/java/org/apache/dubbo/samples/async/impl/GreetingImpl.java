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


import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcContextAttachment;
import org.apache.dubbo.samples.async.GreetingsService;

import java.util.concurrent.CompletableFuture;

public class GreetingImpl implements GreetingsService {
    @Override
    public String sayHi(String name) {
        // 用于从客户端获取附件
        RpcContextAttachment attachmentFromClient = RpcContext.getServerAttachment();
        // 读取客户端发送的附件值
        String received = (String) attachmentFromClient.getAttachment("consumer-key1");
        System.out.println("consumer-key1 from attachment: " + received);

        // 用于向客户端发送附件
        RpcContextAttachment attachmentToClient = RpcContext.getClientAttachment();
        // 设置返回给客户端的附件
        attachmentToClient.setAttachment("server-key1", "server-" + received);

        return "hello, " + name;
    }

    @Override
    public String sayHiAsync(String name) {
        // 用于从客户端获取附件
        RpcContextAttachment attachmentFromClient = RpcContext.getServerAttachment();
        // 读取客户端发送的附件值
        String received = (String) attachmentFromClient.getAttachment("consumer-key1");
        System.out.println("consumer-key1 from attachment: " + received);

        // 用于向客户端发送附件
        RpcContextAttachment attachmentToClient = RpcContext.getClientAttachment();
        // 设置返回给客户端的附件
        attachmentToClient.setAttachment("server-key1", "server-" + received);

        return "hello, " + name;
    }

    @Override
    public CompletableFuture<String> sayHiFuture(String name) {
        // 用于从客户端获取附件
        RpcContextAttachment attachmentFromClient = RpcContext.getServerAttachment();
        // 读取客户端发送的附件值
        String received = (String) attachmentFromClient.getAttachment("consumer-key1");
        System.out.println("consumer-key1 from attachment: " + received);

        // 用于向客户端发送附件
        RpcContextAttachment attachmentToClient = RpcContext.getClientAttachment();
        // 设置返回给客户端的附件
        attachmentToClient.setAttachment("server-key1", "server-" + received);
        return CompletableFuture.completedFuture("hello, " + name);
    }
}
