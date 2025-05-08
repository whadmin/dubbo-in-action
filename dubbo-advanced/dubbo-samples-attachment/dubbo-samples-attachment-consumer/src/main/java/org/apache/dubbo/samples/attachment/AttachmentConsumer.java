package org.apache.dubbo.samples.attachment;

 import org.apache.dubbo.rpc.RpcContext;
 import org.springframework.context.support.ClassPathXmlApplicationContext;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * Dubbo附件传输示例 - 服务消费者
  * 
  * 本类演示了Dubbo消费者如何设置请求附件和获取响应附件。
  * 使用Dubbo 3.x推荐的API方式进行附件操作。
  */
 public class AttachmentConsumer {
     
     private static final Logger logger = LoggerFactory.getLogger(AttachmentConsumer.class);
 
     public static void main(String[] args) throws Exception {
         // 创建并启动Spring上下文
         ClassPathXmlApplicationContext context = null;
         try {
             // 加载Spring配置文件，初始化上下文
             context = new ClassPathXmlApplicationContext("spring/attachment-consumer.xml");
             context.start();
             logger.info("消费者Spring上下文已启动");
             
             // 设置请求附件
             setRequestAttachments();
             
             // 获取远程服务代理并调用
             String result = callRemoteService(context);
             logger.info("远程调用结果: {}", result);
             
             // 获取响应附件
             getResponseAttachments();
             
         } catch (Exception e) {
             logger.error("调用过程中发生异常", e);
         } finally {
             // 确保Spring上下文正确关闭
             if (context != null) {
                 context.close();
                 logger.info("消费者Spring上下文已关闭");
             }
         }
     }
     
     /**
      * 设置请求附件 - 将随RPC请求发送到服务提供者
      */
     private static void setRequestAttachments() {
         // 使用推荐的API设置附件
         RpcContext.getClientAttachment().setAttachment("consumer-key1", "consumer-value1");
         
         // 出于兼容性考虑，同时展示旧API的使用方式
         RpcContext.getServiceContext().setAttachment("consumer-key2", "consumer-value2");
         
         logger.info("已设置请求附件: consumer-key1=consumer-value1, consumer-key2=consumer-value2");
     }
     
     /**
      * 调用远程服务
      * 
      * @param context Spring上下文
      * @return 服务调用结果
      */
     private static String callRemoteService(ClassPathXmlApplicationContext context) {
         // 从Spring上下文获取服务引用
         GreetingsService attachmentService = context.getBean("mockService", GreetingsService.class);
         
         // 执行远程调用 - 设置的附件会随请求发送
         logger.info("开始调用远程服务...");
         String result = attachmentService.sayHi("world");
         logger.info("远程服务调用完成");
         
         return result;
     }
     
     /**
      * 获取响应附件 - 从服务提供者返回的响应中读取附件
      */
     private static void getResponseAttachments() {
         // 记录开始获取附件的日志
         logger.info("开始获取响应附件...");
         
         // 1. 获取 server-key1 附件
         String serverValue1 = RpcContext.getClientResponseContext().getAttachment("server-key1");
         logger.info("通过ClientResponseContext获取: server-key1={}", serverValue1);
         
         // 2. 获取 server-key2 附件
         String serverValue2 = RpcContext.getClientResponseContext().getAttachment("server-key2");
         logger.info("通过ClientResponseContext获取: server-key2={}", serverValue2);
         
         // 记录完成获取附件的日志
         logger.info("响应附件获取完成");
     }
 }
