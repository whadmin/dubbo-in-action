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

package org.apache.dubbo.samples.exception;

/**
 * 问候服务接口，演示异常处理
 */
public interface GreetingsService {
    /**
     * 打招呼方法
     * 
     * @param name 名称
     * @return 问候语
     * @throws BusinessException 当出现业务异常时抛出
     */
    String sayHi(String name) throws BusinessException;
    
    /**
     * 可能抛出超时异常的演示方法
     * 
     * @param timeoutMs 模拟超时的毫秒数
     * @return 处理结果
     */
    String timeoutMethod(int timeoutMs);
    
    /**
     * 可能恢复的异常场景
     * 
     * @param param 参数
     * @return 结果
     * @throws BusinessException 业务异常
     */
    String recoverableMethod(String param) throws BusinessException;
}
