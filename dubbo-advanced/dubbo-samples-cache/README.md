# Dubbo3 缓存配置属性与更新机制

## 缓存配置属性

除了基本的缓存类型选择外，Dubbo3还提供了多种缓存属性来精细化控制缓存行为：

### 1. cache.size（缓存大小）

控制缓存条目的最大数量，防止缓存无限增长导致内存溢出。

```xml
<!-- XML配置方式 -->
<dubbo:reference interface="org.apache.dubbo.samples.cache.GreetingsService" id="cacheService">
    <dubbo:method name="sayHi" cache="lru" cache.size="1000" />
</dubbo:reference>
```

```java
// 注解方式
@DubboReference(methods = {@Method(name = "sayHi", cache = "lru", parameters = {"cache.size", "1000"})})
private GreetingsService greetingsService;
```

```java
// API方式
MethodConfig methodConfig = new MethodConfig();
methodConfig.setName("sayHi");
methodConfig.setCache("lru");
methodConfig.setParameters(Collections.singletonMap("cache.size", "1000"));
```

### 2. cache.expire（过期时间）

设置缓存条目的过期时间，单位为毫秒。超过指定时间后，缓存项将被视为过期，下次请求时将重新调用服务获取结果。

```xml
<dubbo:method name="sayHi" cache="lru" cache.expire="60000" />  <!-- 60秒过期 -->
```

```java
@DubboReference(methods = {@Method(name = "sayHi", cache = "lru", parameters = {"cache.expire", "60000"})})
```

### 3. cache.concurrently（并发控制）

控制是否允许并发更新缓存。设置为`false`时会对同一缓存键的更新操作加锁，避免缓存击穿问题。

```xml
<dubbo:method name="sayHi" cache="lru" cache.concurrently="false" />
```

```java
@DubboReference(methods = {@Method(name = "sayHi", cache = "lru", parameters = {"cache.concurrently", "false"})})
```

### 4. cache.capacity（初始容量）

设置缓存的初始容量，对某些需要预分配空间的缓存实现有效。

```xml
<dubbo:method name="sayHi" cache="lru" cache.capacity="100" />
```

### 5. cache.key.generator（缓存键生成器）

自定义缓存键的生成策略，可以指定实现了`CacheKeyGenerator`接口的类。

```xml
<dubbo:method name="sayHi" cache="lru" cache.key.generator="com.example.CustomKeyGenerator" />
```

## 缓存更新机制

Dubbo的缓存更新机制主要包括以下几种方式：

### 1. 过期更新

缓存项在设定的过期时间（`cache.expire`）后自动失效，下次请求时重新获取最新数据。这是最基本的缓存更新机制。

```xml
<dubbo:method name="sayHi" cache="lru" cache.expire="30000" />  <!-- 30秒后过期 -->
```

### 2. 容量限制更新

当缓存达到最大容量（`cache.size`）时，会根据淘汰策略（如LRU）自动移除最不常用的缓存项，为新数据腾出空间。

### 3. 手动更新

Dubbo目前没有直接提供缓存失效的API，但可以通过以下方式实现手动更新：

#### 方法A：通过自定义缓存实现

```java
public class ManageableCache implements Cache {
    private static final Map<String, ManageableCache> CACHE_INSTANCES = new ConcurrentHashMap<>();
    
    // 构造函数中注册实例
    public ManageableCache() {
        String cacheId = generateCacheId();
        CACHE_INSTANCES.put(cacheId, this);
    }
    
    // 提供静态方法清除指定缓存
    public static void invalidateCache(String cacheId) {
        ManageableCache cache = CACHE_INSTANCES.get(cacheId);
        if (cache != null) {
            cache.clear();
        }
    }
    
    public void clear() {
        // 清除缓存逻辑
    }
}
```

#### 方法B：利用服务版本或分组

当需要清除缓存时，可以通过更改服务版本或分组来实现缓存的间接失效：

```xml
<dubbo:reference interface="..." version="1.0" id="..." />
```

在需要刷新缓存时升级版本到"1.1"。

### 4. 定期刷新机制

可以通过自定义缓存实现定期刷新机制：

```java
public class RefreshableCache implements Cache {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ConcurrentMap<Object, CacheItem> cache = new ConcurrentHashMap<>();
    
    public RefreshableCache(URL url) {
        // 配置定期刷新
        long refreshInterval = url.getParameter("cache.refresh.interval", 300000L); // 默认5分钟
        scheduler.scheduleAtFixedRate(this::refreshAll, refreshInterval, refreshInterval, TimeUnit.MILLISECONDS);
    }
    
    private void refreshAll() {
        // 实现定期刷新逻辑
        cache.clear();
    }
}
```

### 5. 基于事件的缓存更新

可以实现一个基于事件的缓存更新机制，当数据发生变化时发布事件以通知缓存失效：

```java
// 在服务提供方发布事件
@DubboService
public class GreetingsServiceImpl implements GreetingsService {
    @Autowired
    private EventPublisher eventPublisher;
    
    public String updateData(String key, String value) {
        // 更新数据
        String result = doUpdate(key, value);
        // 发布缓存失效事件
        eventPublisher.publishEvent(new CacheInvalidateEvent(key));
        return result;
    }
}

// 在消费方订阅事件并处理
@Component
public class CacheInvalidateListener {
    @EventListener
    public void onCacheInvalidate(CacheInvalidateEvent event) {
        // 使自定义缓存中的相关条目失效
        CustomCache.invalidate(event.getKey());
    }
}
```

在实际应用中，可以根据业务需求和数据特性，选择合适的缓存配置和更新策略，以平衡数据一致性和性能之间的关系。对于一些特殊场景，可能需要结合自定义缓存实现和外部中间件（如Redis）来构建更复杂的缓存更新机制。