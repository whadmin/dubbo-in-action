package org.apache.dubbo.samples.timeout;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.CountDownLatch;

/**
 * 超时示例提供者启动类
 */
public class TimeoutProvider {

    public static void main(String[] args) throws Exception {
        // 启动内嵌的Zookeeper
        new EmbeddedZooKeeper(2181, false).start();
        
        // 加载Spring配置
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/timeout-provider.xml");
        context.start();
        
        System.out.println("超时示例服务提供者已启动...");
        
        // 使应用程序保持运行状态
        new CountDownLatch(1).await();
    }
}
