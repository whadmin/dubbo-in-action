package org.apache.dubbo.samples.attachment;

/**
 * 问候服务接口
 * 
 * 该接口用于演示Dubbo的附件传输功能。
 * 虽然接口本身没有定义任何与附件相关的参数，
 * 但在实际调用过程中可以通过RpcContext传递附件信息。
 */
public interface GreetingsService {

    /**
     * 向指定名称的用户发送问候
     * 
     * @param name 用户名称
     * @return 问候语句
     */
    String sayHi(String name);
}
