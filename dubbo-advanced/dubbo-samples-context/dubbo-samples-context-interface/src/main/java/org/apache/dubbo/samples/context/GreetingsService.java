/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.samples.context;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface GreetingsService {

    /**
     * 基础场景：获取基本的上下文信息
     * 展示获取ServiceContext, ServerAttachment等基本用法
     *
     * @param clientName 客户端名称
     * @return 包含基本上下文信息的响应
     */
    String getBasicInfo(String clientName);

    /**
     * 链路追踪场景：展示如何传递和处理链路追踪信息
     *
     * @param request 请求内容
     * @param traceHeaders 显式传递的追踪头信息
     * @return 包含追踪信息的响应
     */
    String trace(String request, Map<String, String> traceHeaders);

    /**
     * 异步调用场景：展示异步调用中的上下文传递
     *
     * @param request 请求内容
     * @return 异步响应结果
     */
    CompletableFuture<String> getInfoAsync(String request);

    /**
     * 级联调用场景：展示跨服务的上下文传递
     *
     * @param request 请求内容
     * @param needCascade 是否需要级联调用下一级服务
     * @return 包含级联调用信息的响应
     */
    String cascadeCall(String request, boolean needCascade);

    /**
     * 参数传递场景：展示不同类型的参数传递方式
     *
     * @param normalParam 常规参数
     * @param contextParams 显式上下文参数
     * @return 包含参数信息的响应
     */
    String passParameters(String normalParam, Map<String, Object> contextParams);
}
