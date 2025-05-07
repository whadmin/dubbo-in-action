package org.apache.dubbo.samples.mock.consumer;

import org.apache.dubbo.samples.mock.api.DemoService;

public class DemoServiceMock implements DemoService {
    @Override
    public String sayHello(String name) {
        return "Mock result for " + name;
    }
}
