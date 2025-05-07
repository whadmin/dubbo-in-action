package org.apache.dubbo.samples.mock.consumer;

import org.apache.dubbo.samples.mock.api.DemoService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MockConsumer {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/dubbo-mock-consumer.xml");
        context.start();
        
        DemoService demoService = context.getBean("demoService", DemoService.class);
        String result = demoService.sayHello("Mock");
        System.out.println("Result: " + result);
    }
}
