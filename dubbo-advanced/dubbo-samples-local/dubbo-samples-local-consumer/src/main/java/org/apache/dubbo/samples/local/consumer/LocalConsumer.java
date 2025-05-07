package org.apache.dubbo.samples.local.consumer;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.samples.local.api.DemoService;

public class LocalConsumer {
    public static void main(String[] args) {
        ReferenceConfig<DemoService> reference = new ReferenceConfig<>();
        reference.setApplication(new ApplicationConfig("local-consumer"));
        reference.setInterface(DemoService.class);
        // 使用local协议直接调用本地服务
        reference.setUrl("local://localhost/org.apache.dubbo.samples.local.api.DemoService");
        
        DemoService service = reference.get();
        String message = service.sayHello("Local");
        System.out.println(message);
    }
}
