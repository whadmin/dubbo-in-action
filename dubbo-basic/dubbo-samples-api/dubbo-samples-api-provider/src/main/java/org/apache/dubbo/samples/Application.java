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

package org.apache.dubbo.samples;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.samples.api.GreetingsService;
import org.apache.dubbo.samples.provider.GreetingsServiceImpl;

public class Application {

  private static final String ZOOKEEPER_HOST = System.getProperty("zookeeper.address", "127.0.0.1");
  private static final String ZOOKEEPER_PORT = System.getProperty("zookeeper.port", "2181");
  private static final String ZOOKEEPER_ADDRESS =
      "zookeeper://" + ZOOKEEPER_HOST + ":" + ZOOKEEPER_PORT;
  private static final String NACOS_HOST = System.getProperty("nacos.address", "127.0.0.1");
  private static final String NACOS_PORT = System.getProperty("nacos.address", "8848");
  private static final String NACOS_ADDRESS = "nacos://" + NACOS_HOST + ":" + NACOS_PORT;

  public static void main(String[] args) {
    ApplicationConfig applicationConfig = new ApplicationConfig("first-dubbo-provider");
    applicationConfig.setQosPort(33333);

    ServiceConfig<GreetingsService> service = new ServiceConfig<>();
    service.setInterface(GreetingsService.class);
    service.setRef(new GreetingsServiceImpl());

    ProtocolConfig protocolConfig = new ProtocolConfig("dubbo", -1);

    DubboBootstrap.getInstance()
        .application(applicationConfig)
//        .registry(new RegistryConfig(NACOS_ADDRESS))
        .registry(new RegistryConfig(ZOOKEEPER_ADDRESS))
        .protocol(protocolConfig)
        .service(service)
        .start()
        .await();
  }
}
