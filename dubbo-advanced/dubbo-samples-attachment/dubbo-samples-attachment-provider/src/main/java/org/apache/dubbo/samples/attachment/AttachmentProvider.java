package org.apache.dubbo.samples.attachment;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.CountDownLatch;

/**
 * Dubbo附件传输示例 - 服务提供者启动类
 * 
 * 该类负责启动Dubbo服务提供者，包括嵌入式ZooKeeper注册中心和Spring容器
 */
public class AttachmentProvider  {

    public static void main(String[] args) throws Exception {
        // 启动内嵌的ZooKeeper服务，端口为2181，非守护线程模式
        // 这样可以在本地快速启动一个ZooKeeper服务，用于服务注册与发现
        new EmbeddedZooKeeper(2181, false).start();

        // 创建Spring上下文，加载配置文件
        // 配置文件中包含了Dubbo服务的暴露配置
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/attachment-provider.xml");
        // 启动Spring上下文，触发服务暴露
        context.start();

        // 输出服务启动成功的提示信息
        System.out.println("dubbo service started");
        // 使用CountDownLatch阻塞主线程，使服务持续运行
        // 计数器设为1且没有显式调用countDown()，因此会永久阻塞，除非程序被外部中断
        new CountDownLatch(1).await();
    }
}
