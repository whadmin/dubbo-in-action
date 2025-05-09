package org.apache.dubbo.samples.timeout;

import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.samples.timeout.api.TimeoutService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Dubbo超时机制测试类
 */
public class TimeoutServiceTest {

    private TimeoutService timeoutService;

    @Before
    public void setUp() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/timeout-consumer.xml");
        context.start();
        timeoutService = context.getBean("timeoutService", TimeoutService.class);
    }

    /**
     * 测试正常调用，不会触发超时
     */
    @Test
    public void testNormalCall() {
        String result = timeoutService.normalCall();
        assertEquals("正常调用成功", result);
    }

    /**
     * 测试延迟小于超时时间的调用，不会触发超时
     * 消费者超时设置为2000ms，延迟1000ms
     */
    @Test
    public void testTimeoutCallWithinLimit() {
        String result = timeoutService.serviceLevelTimeout(1000);
        assertEquals("延迟 1000 毫秒后调用成功", result);
    }

    /**
     * 测试延迟大于超时时间的调用，会触发超时异常
     * 消费者超时设置为2000ms，延迟3000ms
     */
    @Test(expected = RpcException.class)
    public void testTimeoutCallExceedLimit() {
        // 这里会抛出RpcException
        timeoutService.serviceLevelTimeout(3000);
    }

    /**
     * 测试方法级别的超时配置
     * 方法级别超时设置为5000ms，延迟4000ms，不会触发超时
     */
    @Test
    public void testMethodLevelTimeout() {
        String result = timeoutService.methodLevelTimeout(4000);
        assertEquals("方法级别超时设置测试：延迟 4000 毫秒后调用成功", result);
    }
    
    /**
     * 测试方法级别的超时配置（超时情况）
     * 方法级别超时设置为5000ms，延迟6000ms，会触发超时
     */
    @Test(expected = RpcException.class)
    public void testMethodLevelTimeoutExceed() {
        timeoutService.methodLevelTimeout(6000);
        fail("应该抛出RpcException");
    }
    

}
