package org.apache.dubbo.samples.context.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcServiceContext;
import org.apache.dubbo.samples.context.GreetingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 问候服务接口实现类
 * 
 * 该实现演示了Dubbo中各种上下文信息的使用场景，包括：
 * - 基础信息获取
 * - 链路追踪
 * - 异步调用
 * - 级联调用
 * - 参数传递
 */
public class GreetingImpl implements GreetingsService {
    private static final Logger logger = LoggerFactory.getLogger(GreetingImpl.class);
    private static final ExecutorService ASYNC_EXECUTOR = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors() * 2,
        r -> {
            Thread t = new Thread(r, "AsyncServiceExecutor-" + r.hashCode());
            t.setDaemon(true);
            return t;
        }
    );
    private final GreetingsService nextService = this;

    /**
     * 基础场景：获取基本的上下文信息
     * 展示获取ServiceContext, ServerAttachment等基本用法
     * 
     * @param clientName 客户端名称
     * @return 包含基本信息的响应字符串
     */
    @Override
    public String getBasicInfo(String clientName) {
        // 记录处理开始时间
        long startTime = System.currentTimeMillis();
        logger.debug("开始处理来自 {} 的基础信息请求", clientName);

        try {
            // 获取ServiceContext
            RpcServiceContext serviceContext = RpcContext.getServiceContext();

            // 可用的API
            boolean isProviderSide = serviceContext.isProviderSide();  // 是否为服务提供方
            String remoteHost = serviceContext.getRemoteHost();        // 获取远程主机Host
            String remoteAddress =serviceContext.getRemoteAddress().toString(); // 获取远程主机地址
            String remoteApplication = serviceContext.getRemoteApplicationName(); // 获取远程应用名
            String localHost = serviceContext.getLocalHost();          // 获取本地主机地址
            String localAddress = serviceContext.getLocalAddress().toString(); // 获取ServiceContext信息
            String InterfaceName = serviceContext.getInterfaceName(); // 获取接口类
            String methodName = serviceContext.getMethodName();        // 获取当前调用的方法名
            URL url = serviceContext.getUrl();                         // 获取URL

            // 构建响应
            StringBuilder response = new StringBuilder(256);
            response.append("Hello ").append(clientName).append(", Basic Information:\n");
            response.append("Provider Side: ").append(isProviderSide).append("\n");
            response.append("Remote Host: ").append(remoteHost).append("\n");
            response.append("Remote Address: ").append(remoteAddress).append("\n");
            response.append("Remote Application: ").append(remoteApplication).append("\n");
            response.append("Local Host: ").append(localHost).append("\n");
            response.append("Local Address: ").append(localAddress).append("\n");
            response.append("Interface Name: ").append(InterfaceName).append("\n");
            response.append("Method Name: ").append(methodName).append("\n");
            response.append("url: ").append(url.toString()).append("\n");
            response.append("Processing Time: ").append(System.currentTimeMillis() - startTime).append("ms\n");

            return response.toString();
        } catch (Exception e) {
            logger.error("处理基础信息请求时发生错误", e);
            return "Error processing request: " + e.getMessage();
        } finally {
            logger.debug("完成处理来自 {} 的基础信息请求，耗时: {}ms", 
                    clientName, System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 链路追踪场景：展示如何传递和处理链路追踪信息
     * 
     * @param request 请求内容
     * @param traceHeaders 追踪头信息
     * @return 追踪响应信息
     */
    @Override
    public String trace(String request, Map<String, String> traceHeaders) {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        String traceId = null;
        String spanId = null;
        String parentSpanId = null;
        
        try {
            logger.debug("开始处理追踪请求: {}", request);
            
            // 获取追踪信息，优先使用显式传递的信息
            traceId = getTraceValue(traceHeaders, "traceId");
            spanId = getTraceValue(traceHeaders, "spanId");
            parentSpanId = getTraceValue(traceHeaders, "parentSpanId");

            // 生成当前服务的跟踪信息
            String currentSpanId = (spanId != null) ? spanId + ".1" : "1";

            // 构建响应
            StringBuilder response = new StringBuilder(256);
            response.append("Trace Response for: ").append(request).append("\n");
            response.append("TraceId: ").append(traceId != null ? traceId : "Not provided").append("\n");
            response.append("ParentSpanId: ").append(parentSpanId != null ? parentSpanId : "Not provided").append("\n");
            response.append("SpanId: ").append(spanId != null ? spanId : "Not provided").append("\n");
            response.append("CurrentSpanId: ").append(currentSpanId).append("\n");
            response.append("Processing Time: ").append(System.currentTimeMillis() - startTime).append("ms\n");

            // 记录追踪信息日志
            logger.info("===== 追踪处理日志 =====\n{}\n=================", response);

            return response.toString();
        } catch (Exception e) {
            logger.error("处理追踪请求时发生错误, traceId: {}", traceId, e);
            return "Error processing trace request: " + e.getMessage();
        } finally {
            logger.debug("完成处理追踪请求, 耗时: {}ms, traceId: {}", 
                    System.currentTimeMillis() - startTime, traceId);
        }
    }

    /**
     * 异步调用场景：展示异步调用中的上下文传递
     * 
     * @param request 请求内容
     * @return 异步响应结果
     */
    @Override
    public CompletableFuture<String> getInfoAsync(String request) {
        logger.debug("接收异步处理请求: {}", request);
        
        // 在主线程中捕获上下文
        final String traceId = RpcContext.getServerAttachment().getAttachment("traceId");
        final String asyncMarker = RpcContext.getServerAttachment().getAttachment("asyncMarker");
        
        // 使用自定义的线程池提高性能
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            logger.debug("开始异步处理, traceId: {}", traceId);
            
            try {
                // 模拟处理时间
                Thread.sleep(100);
                
                StringBuilder response = new StringBuilder(256);
                response.append("Async Response for: ").append(request).append("\n");
                response.append("Processed in thread: ").append(Thread.currentThread().getName()).append("\n");
                response.append("TraceId from original request: ").append(traceId != null ? traceId : "Not provided").append("\n");
                response.append("Async Marker: ").append(asyncMarker != null ? asyncMarker : "Not provided").append("\n");
                response.append("Processing Time: ").append(System.currentTimeMillis() - startTime).append("ms\n");
                
                logger.debug("异步处理完成, traceId: {}, 耗时: {}ms", 
                        traceId, System.currentTimeMillis() - startTime);
                return response.toString();
            } catch (Exception e) {
                logger.error("异步处理失败, traceId: {}", traceId, e);
                return "Async processing failed: " + e.getMessage();
            }
        }, ASYNC_EXECUTOR);
    }

    /**
     * 级联调用场景：展示跨服务的上下文传递
     * 
     * @param request 请求内容
     * @param needCascade 是否需要级联调用
     * @return 级联调用响应
     */
    @Override
    public String cascadeCall(String request, boolean needCascade) {
        long startTime = System.currentTimeMillis();
        logger.debug("接收级联调用请求: {}, needCascade: {}", request, needCascade);
        
        try {
            // 获取上下文信息
            String traceId = RpcContext.getServerAttachment().getAttachment("traceId");
            String spanId = RpcContext.getServerAttachment().getAttachment("spanId");
            String level = RpcContext.getServerAttachment().getAttachment("level");
            
            // 如果traceId为空，生成新的traceId
            if (traceId == null) {
                traceId = "Generated-" + System.currentTimeMillis();
            }

            // 生成当前服务的跟踪信息
            String currentSpanId = (spanId != null) ? spanId + ".1" : "1";
            String currentLevel = (level != null) ? level : "1";

            StringBuilder response = new StringBuilder(256);
            response.append("Cascade Response Level ").append(currentLevel).append(" for: ").append(request).append("\n");
            response.append("TraceId: ").append(traceId).append("\n");
            response.append("CurrentSpanId: ").append(currentSpanId).append("\n");

            // 如果需要级联调用下一级服务
            if (needCascade) {
                String nextLevelResponse = processCascadeCall(request, traceId, currentSpanId, currentLevel);
                response.append("\n--- Next Level Response ---\n");
                response.append(nextLevelResponse);
            }

            logger.debug("级联调用处理完成, traceId: {}, 耗时: {}ms", 
                    traceId, System.currentTimeMillis() - startTime);
            return response.toString();
        } catch (Exception e) {
            logger.error("级联调用处理失败, request: {}", request, e);
            return "Cascade call error: " + e.getMessage();
        }
    }

    /**
     * 参数传递场景：展示不同类型的参数传递方式
     * 
     * @param normalParam 常规参数
     * @param contextParams 上下文参数Map
     * @return 参数处理响应
     */
    @Override
    public String passParameters(String normalParam, Map<String, Object> contextParams) {
        long startTime = System.currentTimeMillis();
        logger.debug("接收参数传递请求, 常规参数: {}", normalParam);
        
        try {
            // 构建响应对象
            StringBuilder response = new StringBuilder(256);
            response.append("参数传递演示:\n");
            
            // 处理各类参数并构建响应内容
            processNormalParameter(response, normalParam);
            processExplicitContextParameters(response, contextParams);
            processImplicitContextParameters(response);
            
            // 添加处理时间信息
            response.append("处理耗时: ").append(System.currentTimeMillis() - startTime).append("毫秒\n");
            
            // 记录日志并返回结果
            logger.debug("参数传递处理完成, 耗时: {}ms", System.currentTimeMillis() - startTime);
            return response.toString();
        } catch (Exception e) {
            logger.error("参数传递处理失败, 常规参数: {}", normalParam, e);
            return "参数处理错误: " + e.getMessage();
        }
    }

    /**
     * 处理常规参数
     * 
     * @param response 响应构建器
     * @param normalParam 常规参数
     */
    private void processNormalParameter(StringBuilder response, String normalParam) {
        response.append("常规参数: ").append(normalParam).append("\n");
    }

    /**
     * 处理显式上下文参数
     * 
     * @param response 响应构建器
     * @param contextParams 上下文参数Map
     */
    private void processExplicitContextParameters(StringBuilder response, Map<String, Object> contextParams) {
        response.append("显式上下文参数:\n");
        
        if (contextParams != null && !contextParams.isEmpty()) {
            for (Map.Entry<String, Object> entry : contextParams.entrySet()) {
                response.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        } else {
            response.append("  未提供显式参数\n");
        }
    }

    /**
     * 处理隐式上下文参数
     * 
     * @param response 响应构建器
     */
    private void processImplicitContextParameters(StringBuilder response) {
        response.append("隐式上下文参数:\n");
        
        Map<String, String> attachments = RpcContext.getServerAttachment().getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            for (Map.Entry<String, String> entry : attachments.entrySet()) {
                response.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        } else {
            response.append("  未提供隐式参数\n");
        }
    }

    /**
     * 格式化附件信息为可读字符串
     * 
     * @param attachments 附件Map
     * @return 格式化后的附件字符串
     */
    private String formatAttachments(Map<String, String> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return "[]";
        }
        
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (Map.Entry<String, String> entry : attachments.entrySet()) {
            joiner.add(entry.getKey() + "=" + entry.getValue());
        }
        return joiner.toString();
    }

    /**
     * 获取追踪值，优先从显式传递的Map中获取，其次从RpcContext中获取
     * 
     * @param traceHeaders 追踪头信息Map
     * @param key 键名
     * @return 追踪值，如果不存在则返回null
     */
    private String getTraceValue(Map<String, String> traceHeaders, String key) {
        String value = null;
        
        // 先从RpcContext中获取
        value = RpcContext.getServerAttachment().getAttachment(key);
        
        // 如果traceHeaders不为空，则优先使用显式传递的值
        if (traceHeaders != null && traceHeaders.containsKey(key)) {
            value = traceHeaders.get(key);
        }
        
        return value;
    }

    /**
     * 处理级联调用逻辑
     * 
     * @param request 原始请求
     * @param traceId 追踪ID
     * @param currentSpanId 当前Span ID
     * @param currentLevel 当前级别
     * @return 级联调用结果
     */
    private String processCascadeCall(String request, String traceId, String currentSpanId, String currentLevel) {
        try {
            // 计算下一级别
            int nextLevel = Integer.parseInt(currentLevel) + 1;
            
            // 设置传递给下一级服务的上下文信息
            RpcContext.getClientAttachment().setAttachment("traceId", traceId);
            RpcContext.getClientAttachment().setAttachment("parentSpanId", currentSpanId);
            RpcContext.getClientAttachment().setAttachment("spanId", currentSpanId + ".1");
            RpcContext.getClientAttachment().setAttachment("level", String.valueOf(nextLevel));

            // 级联调用（避免无限递归，第二个参数传false）
            return nextService.cascadeCall("Cascaded-" + request, false);
        } catch (Exception e) {
            logger.error("级联调用下一级服务失败, traceId: {}", traceId, e);
            return "Cascade call to next level failed: " + e.getMessage();
        }
    }
}