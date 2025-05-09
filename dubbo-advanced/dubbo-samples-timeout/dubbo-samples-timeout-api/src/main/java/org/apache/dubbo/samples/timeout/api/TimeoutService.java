package org.apache.dubbo.samples.timeout.api;

/**
 * 超时服务接口
 * 用于演示Dubbo超时机制
 */
public interface TimeoutService {
    
    /**
     * 正常执行方法，不会超时
     * 
     * @return 执行结果
     */
    String normalCall();
    
    /**
     * 延迟执行方法，可能导致超时
     * 
     * @param delayMs 延迟执行的毫秒数
     * @return 执行结果
     */
    String serviceLevelTimeout(int delayMs);
    
    /**
     * 方法级别指定超时时间的方法
     * 
     * @param delayMs 延迟执行的毫秒数
     * @return 执行结果
     */
    String methodLevelTimeout(int delayMs);
    
}
