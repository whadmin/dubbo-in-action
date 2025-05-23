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

package org.apache.dubbo.samples.exception.impl;

import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.samples.exception.BusinessException;
import org.apache.dubbo.samples.exception.GreetingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * 问候服务实现，演示异常处理
 */
public class GreetingImpl implements GreetingsService {
    private static final Logger logger = LoggerFactory.getLogger(GreetingImpl.class);
    private Random random = new Random();
    
    @Override
    public String sayHi(String name) throws BusinessException {
        logger.info("执行sayHi方法，参数: {}", name);
        
        // 记录调用方IP等信息
        logger.info("调用方IP: {}", RpcContext.getServiceContext().getRemoteHost());
        logger.info("调用方应用: {}", RpcContext.getServiceContext().getRemoteApplicationName());
        
        // 模拟业务异常
        if ("exception".equals(name)) {
            logger.error("触发业务异常场景");
            throw new BusinessException("BIZ_ERROR", "业务处理异常: 无效的名称参数");
        }
        
        // 模拟运行时异常
        if ("runtime".equals(name)) {
            logger.error("触发运行时异常场景");
            throw new RuntimeException("意外的运行时异常");
        }
        
        // 模拟空指针等系统异常
        if ("null".equals(name)) {
            logger.error("触发空指针异常场景");
            String s = null;
            return s.toString(); // 将触发NPE
        }
        
        // 正常业务逻辑
        return "你好, " + name;
    }

    @Override
    public String timeoutMethod(int timeoutMs) {
        logger.info("执行timeoutMethod方法，延迟: {}ms", timeoutMs);
        
        try {
            // 模拟耗时操作
            Thread.sleep(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "处理被中断";
        }
        
        return "处理完成，耗时: " + timeoutMs + "ms";
    }

    @Override
    public String recoverableMethod(String param) throws BusinessException {
        logger.info("执行recoverableMethod方法，参数: {}", param);
        
        // 模拟随机失败，用于测试重试机制
        if ("recoverable".equals(param) && random.nextInt(4) > 0) { // 75%的概率失败
            if (random.nextBoolean()) {
                logger.warn("模拟网络故障");
                throw new RuntimeException("网络连接断开");
            } else {
                logger.warn("模拟超时");
                try {
                    Thread.sleep(2000); // 模拟超时
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "处理超时";
            }
        }
        
        return "恢复处理成功: " + param;
    }
}
