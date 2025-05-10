# Dubbo3 RpcContext API 全面详解：上下文传递的利器

## 1. 引言

在微服务架构中，上下文信息传递是一个核心需求。当跨服务调用时，我们通常需要传递诸如用户身份、追踪ID、调用链信息等数据。Apache Dubbo 作为一款高性能的 RPC 框架，通过 `RpcContext` 提供了强大的上下文传递功能。

本文将深入解析 Dubbo3 的 `RpcContext` API，帮助你掌握这一微服务开发的关键工具。

## 2. RpcContext 概述

### 2.1 什么是 RpcContext？

`RpcContext` 是 Dubbo 提供的一个上下文传递工具，它允许开发者在 RPC 调用过程中隐式传递和共享信息，而无需修改方法签名。

在 Dubbo3 中，为了支持更多场景并提高可维护性，`RpcContext` 被重构为多个独立的上下文容器，每个容器负责特定的功能域。

### 2.2 RpcContext 在 Dubbo3 中的变化

Dubbo3 对 RpcContext 进行了重大改进。相比 Dubbo2.x 中单一的 `RpcContext.getContext()` API，Dubbo3 将上下文分为以下几类：

```java
// 获取服务调用相关的上下文信息（如调用方IP、方法名等）
RpcContext.getServiceContext()

// 获取服务端的附件信息（从消费者传递过来的）
RpcContext.getServerAttachment()

// 设置要传递给服务端的附件信息
RpcContext.getClientAttachment()

// 获取/设置服务端的上下文信息（同一次请求内共享，可跨多次RPC传递）
RpcContext.getServerContext()

// 获取/设置客户端的上下文信息（同一次请求内共享，可跨多次RPC传递）
RpcContext.getClientContext()
```

这种分离设计使上下文的使用更加清晰，避免了之前版本中参数混用的问题。

## 3. RpcContext API 详解

### 3.1 ServiceContext：调用元数据

`ServiceContext` 提供了与当前调用相关的元数据，主要用于获取调用信息，而非传递数据。

```java
// 获取ServiceContext
RpcContext.ServiceContext serviceContext = RpcContext.getServiceContext();

// 可用的API
boolean isProviderSide = serviceContext.isProviderSide();  // 是否为服务提供方
boolean isConsumerSide = serviceContext.isConsumerSide();  // 是否为服务消费方
String methodName = serviceContext.getMethodName();        // 获取当前调用的方法名
Class<?> interfaceClass = serviceContext.getInterfaceClass(); // 获取接口类
String remoteHost = serviceContext.getRemoteHost();        // 获取远程主机地址
String remoteApplication = serviceContext.getRemoteApplicationName(); // 获取远程应用名
String localHost = serviceContext.getLocalHost();          // 获取本地主机地址
URL url = serviceContext.getUrl();                         // 获取URL
Invoker<?> invoker = serviceContext.getInvoker();          // 获取Invoker
```

使用示例：

```java
@Override
public String getBasicInfo(String clientName) {
    // 获取ServiceContext信息
    boolean isProviderSide = RpcContext.getServiceContext().isProviderSide();
    String clientIP = RpcContext.getServiceContext().getRemoteHost();
    String localAddress = RpcContext.getServiceContext().getLocalAddress().toString();
    String methodName = RpcContext.getServiceContext().getMethodName();
    String remoteApplication = RpcContext.getServiceContext().getRemoteApplicationName();
    
    // ... 处理业务逻辑
}
```

### 3.2 ServerAttachment：接收隐式参数

`ServerAttachment` 用于服务提供方接收消费方传递的附件信息，这些附件是通过 `ClientAttachment` 传递的。

```java
// 获取ServerAttachment
RpcContext.ServerAttachment serverAttachment = RpcContext.getServerAttachment();

// 获取所有附件
Map<String, String> attachments = serverAttachment.getAttachments();

// 获取特定附件
String traceId = serverAttachment.getAttachment("traceId");
String userId = serverAttachment.getAttachment("userId");

// 获取附件，提供默认值
String clientVersion = serverAttachment.getAttachment("clientVersion", "unknown");
```

使用示例：

```java
@Override
public String trace(String request, Map<String, String> traceHeaders) {
    // 获取追踪信息
    String traceId = RpcContext.getServerAttachment().getAttachment("traceId");
    String spanId = RpcContext.getServerAttachment().getAttachment("spanId");
    
    // ... 处理追踪逻辑
}
```

### 3.3 ClientAttachment：发送隐式参数

`ClientAttachment` 用于服务消费方向提供方传递附件信息，是隐式传参的主要工具。

```java
// 获取ClientAttachment
RpcContext.ClientAttachment clientAttachment = RpcContext.getClientAttachment();

// 设置附件（需要在每次RPC调用前设置）
clientAttachment.setAttachment("traceId", UUID.randomUUID().toString());
clientAttachment.setAttachment("userId", "10001");
clientAttachment.setAttachment("clientVersion", "2.0");

// 设置多个附件
Map<String, String> attachments = new HashMap<>();
attachments.put("app", "myApp");
attachments.put("token", "xyz123");
clientAttachment.setAttachments(attachments);

// 清除特定附件
clientAttachment.removeAttachment("token");

// 清除所有附件
clientAttachment.clearAttachments();
```

使用示例：

```java
// 设置要传递给服务提供方的附件
RpcContext.getClientAttachment().setAttachment("clientId", "consumer-" + System.currentTimeMillis());
RpcContext.getClientAttachment().setAttachment("userId", "user_" + System.currentTimeMillis() % 1000);

// 调用远程服务
String result = contextService.getBasicInfo("BasicInfoClient");
```

### 3.4 ServerContext：服务端可传递上下文

`ServerContext` 用于在服务提供方内保存上下文数据，特别适合在同一请求处理过程中跨越多个服务方法调用时共享数据。

```java
// 获取ServerContext
RpcContext.ServerContext serverContext = RpcContext.getServerContext();

// 设置上下文数据（注意：值不限于String类型）
serverContext.set("startTime", System.currentTimeMillis());
serverContext.set("user", userObject);

// 获取上下文数据
long startTime = (long) serverContext.get("startTime");
User user = (User) serverContext.get("user");

// 获取所有上下文数据
Map<String, Object> values = serverContext.get();

// 移除上下文数据
serverContext.remove("startTime");
```

### 3.5 ClientContext：客户端可传递上下文

`ClientContext` 与 `ServerContext` 类似，但用于服务消费方存储上下文数据。

```java
// 获取ClientContext
RpcContext.ClientContext clientContext = RpcContext.getClientContext();

// 设置上下文数据
clientContext.set("requestId", UUID.randomUUID().toString());
clientContext.set("startTime", System.currentTimeMillis());

// 获取上下文数据
String requestId = (String) clientContext.get("requestId");
```

## 4. RpcContext 使用场景实战

### 4.1 基础场景：获取调用元信息

当需要了解当前调用的基本信息，如调用方IP、方法名称等时，可以使用 `ServiceContext`：

```java
@Override
public String getServiceInfo() {
    StringBuilder info = new StringBuilder();
    
    // 获取调用元信息
    info.append("调用方IP: ").append(RpcContext.getServiceContext().getRemoteHost()).append("\n");
    info.append("调用方应用: ").append(RpcContext.getServiceContext().getRemoteApplicationName()).append("\n");
    info.append("调用方法: ").append(RpcContext.getServiceContext().getMethodName()).append("\n");
    info.append("服务端点: ").append(RpcContext.getServiceContext().getLocalAddress()).append("\n");
    
    return info.toString();
}
```

### 4.2 分布式追踪场景

分布式追踪是微服务架构中的重要需求，可以通过 `RpcContext` 传递追踪ID等信息：

```java
// 消费者端
String traceId = UUID.randomUUID().toString();
RpcContext.getClientAttachment().setAttachment("traceId", traceId);
RpcContext.getClientAttachment().setAttachment("spanId", "1");
service.methodA("参数");

// 提供者端
@Override
public void methodA(String param) {
    String traceId = RpcContext.getServerAttachment().getAttachment("traceId");
    String spanId = RpcContext.getServerAttachment().getAttachment("spanId");
    
    // 记录日志
    logger.info("处理请求, traceId: {}, spanId: {}", traceId, spanId);
    
    // 如果需要继续调用其他服务
    RpcContext.getClientAttachment().setAttachment("traceId", traceId);
    RpcContext.getClientAttachment().setAttachment("spanId", spanId + ".1");
    otherService.methodB();
}
```

### 4.3 异步调用场景

在异步调用中，需要特别注意上下文的传递。由于异步执行跨线程，需要在主线程中先获取上下文信息：

```java
@Override
public CompletableFuture<String> getInfoAsync(String request) {
    // 在主线程中捕获上下文
    final String traceId = RpcContext.getServerAttachment().getAttachment("traceId");
    
    // 使用自定义线程池
    return CompletableFuture.supplyAsync(() -> {
        // 在异步线程中不能直接访问RpcContext，使用之前保存的值
        logger.info("异步处理, traceId: {}", traceId);
        return "异步处理完成, traceId: " + traceId;
    }, executorService);
}
```

### 4.4 级联调用场景

当服务A调用服务B，服务B又调用服务C时，如何保持上下文信息的连贯性是一个常见问题：

```java
// 消费者调用第一个服务
RpcContext.getClientAttachment().setAttachment("traceId", "cascadeTrace123");
RpcContext.getClientAttachment().setAttachment("level", "1");
serviceA.methodA();

// 服务A实现
@Override
public void methodA() {
    // 获取上下文
    String traceId = RpcContext.getServerAttachment().getAttachment("traceId");
    String level = RpcContext.getServerAttachment().getAttachment("level");
    
    // 处理业务逻辑...
    
    // 调用服务B时传递上下文
    RpcContext.getClientAttachment().setAttachment("traceId", traceId);
    RpcContext.getClientAttachment().setAttachment("level", String.valueOf(Integer.parseInt(level) + 1));
    serviceB.methodB();
}
```

### 4.5 显式参数与隐式参数结合

在某些场景下，需要同时使用显式参数和隐式参数：

```java
// 消费者端
Map<String, Object> contextParams = new HashMap<>();
contextParams.put("bizParam1", "value1");

// 设置隐式参数
RpcContext.getClientAttachment().setAttachment("traceId", "trace123");

// 调用服务
service.process("mainParam", contextParams);

// 提供者端
@Override
public void process(String mainParam, Map<String, Object> contextParams) {
    // 处理显式参数
    System.out.println("主参数: " + mainParam);
    System.out.println("业务参数1: " + contextParams.get("bizParam1"));
    
    // 处理隐式参数
    String traceId = RpcContext.getServerAttachment().getAttachment("traceId");
    System.out.println("追踪ID: " + traceId);
}
```

## 5. RpcContext 注意事项与最佳实践

### 5.1 生命周期限制

`RpcContext` 的附件信息（ClientAttachment, ServerAttachment）仅在当前调用有效，每次RPC调用后会自动清空。

```java
// 这样做是无效的
RpcContext.getClientAttachment().setAttachment("key", "value");
service.method1();  // 这次调用会收到附件

service.method2();  // 这次调用不会收到附件，因为已被清空
```

正确做法：

```java
RpcContext.getClientAttachment().setAttachment("key", "value");
service.method1();  // 第一次调用

RpcContext.getClientAttachment().setAttachment("key", "value");  // 需要重新设置
service.method2();  // 第二次调用
```

### 5.2 异步调用注意事项

在异步调用中，由于线程切换，不能在异步线程中直接访问 `RpcContext`：

```java
// 错误示例
@Override
public CompletableFuture<String> asyncMethod() {
    return CompletableFuture.supplyAsync(() -> {
        // 错误：这里无法获取到正确的上下文信息
        String traceId = RpcContext.getServerAttachment().getAttachment("traceId");
        return process(traceId);
    });
}

// 正确示例
@Override
public CompletableFuture<String> asyncMethod() {
    // 在提交异步任务前先获取上下文
    final String traceId = RpcContext.getServerAttachment().getAttachment("traceId");
    
    return CompletableFuture.supplyAsync(() -> {
        // 使用之前保存的上下文信息
        return process(traceId);
    });
}
```

### 5.3 类型限制

`ClientAttachment` 和 `ServerAttachment` 中的附件值只能是 String 类型，如需传递复杂对象，可以：

1. 使用JSON序列化为字符串
2. 使用Base64编码二进制数据
3. 改用显式参数传递

而 `ClientContext` 和 `ServerContext` 支持任意类型的对象。

### 5.4 最佳实践

1. **根据用途选择正确的上下文类型**
   - 获取调用信息：使用 `ServiceContext`
   - 传递隐式参数：使用 `ClientAttachment`/`ServerAttachment`
   - 保存请求级别的数据：使用 `ClientContext`/`ServerContext`

2. **合理选择显式和隐式参数**
   - 业务核心参数：使用显式参数（方法参数）
   - 横切关注点（如跟踪ID）：使用隐式参数（RpcContext附件）

3. **附件命名规范**
   - 使用有意义的前缀避免冲突，如：`trace.id`, `auth.token`
   - 对于自定义附件，使用应用名作为前缀，如：`myapp.userId`

4. **异步场景处理**
   - 始终在提交异步任务前保存所需的上下文信息
   - 考虑使用MDC等工具在日志中关联上下文信息

## 6. RpcContext与其他技术的集成

### 6.1 与分布式追踪系统集成

RpcContext 可以无缝集成各种分布式追踪系统，如 SkyWalking、Zipkin 等：

```java
// 拦截器获取追踪信息
public class TracingFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 从追踪系统获取当前span信息
        String traceId = TracingSystem.getCurrentTraceId();
        String spanId = TracingSystem.getCurrentSpanId();
        
        // 通过RpcContext传递
        RpcContext.getClientAttachment().setAttachment("traceId", traceId);
        RpcContext.getClientAttachment().setAttachment("spanId", spanId);
        
        return invoker.invoke(invocation);
    }
}
```

### 6.2 与认证授权系统集成

RpcContext 可以传递认证信息，实现透明的分布式认证：

```java
// 消费者端拦截器
public class AuthFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 获取当前用户的认证token
        String token = AuthContext.getCurrentUserToken();
        
        // 通过RpcContext传递
        RpcContext.getClientAttachment().setAttachment("auth.token", token);
        
        return invoker.invoke(invocation);
    }
}

// 提供者端拦截器
public class AuthValidationFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 从RpcContext获取认证信息
        String token = RpcContext.getServerAttachment().getAttachment("auth.token");
        
        // 验证token
        if (!authService.validate(token)) {
            throw new RpcException("认证失败");
        }
        
        return invoker.invoke(invocation);
    }
}
```

## 7. 常见问题与排查

### 7.1 附件无法传递

**问题**：设置了ClientAttachment，但在服务端无法通过ServerAttachment获取。

**排查**：
1. 确认附件是否在正确的位置设置（每次RPC调用前）
2. 检查是否使用了正确的上下文类型（ClientAttachment vs ClientContext）
3. 验证Dubbo版本是否一致，尤其是从2.x升级到3.x的情况
4. 检查是否有Filter清除了附件

### 7.2 异步调用上下文丢失

**问题**：在异步方法中无法获取RpcContext信息。

**解决**：
```java
// 在提交异步任务前保存上下文
String traceId = RpcContext.getServerAttachment().getAttachment("traceId");

return CompletableFuture.supplyAsync(() -> {
    // 使用之前保存的上下文，不要直接读取RpcContext
    doSomething(traceId);
});
```

### 7.3 级联调用信息断链

**问题**：在多级服务调用中，上下文信息没有正确传递。

**解决**：
```java
// 服务A中
String traceId = RpcContext.getServerAttachment().getAttachment("traceId");
// 处理逻辑...

// 调用服务B时，需要重新设置ClientAttachment
RpcContext.getClientAttachment().setAttachment("traceId", traceId);
serviceB.method();
```

## 8. 总结

Dubbo3 的 RpcContext API 提供了强大而灵活的上下文传递机制，通过分离不同类型的上下文，使得设计更加清晰，功能更加丰富。掌握这些 API 的正确使用方式，将大大提升你在微服务架构中处理跨服务信息传递的能力。

在实际应用中，应根据实际需求选择合适的上下文类型，并注意附件的生命周期和线程安全问题。特别是在异步调用场景，需要格外注意上下文的正确传递。

通过本文的详细解析和实战示例，相信你已经可以充分利用 Dubbo3 的 RpcContext API，构建更加健壮的微服务应用。

---

**参考资料**：
- [Apache Dubbo 官方文档 - RpcContext](https://dubbo.apache.org/zh/docs3-v2/java-sdk/reference-manual/spi/context/)
- [Apache Dubbo GitHub 仓库](https://github.com/apache/dubbo)
- [分布式链路追踪最佳实践](https://dubbo.apache.org/zh/blog/2020/05/18/分布式链路追踪实践/)