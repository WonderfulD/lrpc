# LRPC (Light-RPC)
## 介绍
LRPC 是一款轻量级、高性能的 RPC 框架，深度集成 Spring Boot，提供易用且强大的远程服务调用能力。通过模块化设计和插件化架构，LRPC 可以根据不同业务场景灵活配置，实现高可用、高性能的分布式服务通信。
## 特性
- 轻量级: 框架核心设计简洁，易于理解和使用，性能开销低
- 高扩展性: 基于 SPI 机制、Spring依赖注入实现插件化架构，支持多种组件自由切换
- 多协议支持: 基于接口实现，支持Tomcat、Netty等多种底层通信方式
- 多注册中心: 灵活对接 Redis、Nacos 等多种注册中心实现
- 高可用性: 内置多种重试策略和负载均衡算法
- Spring Boot 集成: 提供 starter 组件，一键集成到 Spring Boot 应用
## 使用说明

### Maven 依赖配置

在您的 Spring Boot 项目中添加以下依赖：

```xml
<!-- 必须引入：LRPC Spring Boot Starter -->
<dependency>
    <groupId>space.ruiwang</groupId>
    <artifactId>rpc-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<!-- 选择注册中心实现 (二选一) -->
<!-- Nacos 注册中心 -->
<dependency>
    <groupId>space.ruiwang</groupId>
    <artifactId>rpc-register-nacos</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
<!-- 或 Redis 注册中心 -->
<!--
<dependency>
    <groupId>space.ruiwang</groupId>
    <artifactId>rpc-register-redis</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
-->

<!-- 选择通信层实现 (二选一) -->
<!-- Netty 通信 (推荐) -->
<dependency>
    <groupId>space.ruiwang</groupId>
    <artifactId>rpc-transport-netty</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
<!-- 或 Tomcat 通信 -->
<!--
<dependency>
    <groupId>space.ruiwang</groupId>
    <artifactId>rpc-transport-tomcat</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
-->

<!-- 共享服务接口 (Provider 和 Consumer 共用) -->
<dependency>
    <groupId>space.ruiwang</groupId>
    <artifactId>rpc-interface</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### JVM 参数

启动 Provider、Consumer 需要添加以下 JVM 参数：

```bash
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/jdk.internal.misc=ALL-UNNAMED
--add-opens java.base/java.nio=ALL-UNNAMED
--add-opens java.base/sun.nio.ch=ALL-UNNAMED
-Dio.netty.tryReflectionSetAccessible=true
```

### Provider 端示例

```java
@SpringBootApplication
@EnableProviderRpc
public class RpcProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(RpcProviderApplication.class, args);
    }
}

@RpcService(interfaceClass = TestService.class, serviceVersion = "1.0")
public class TestServiceImpl implements TestService {
    @Override
    public int calc(int a, int b) {
        return a + b;
    }
}
```

### Consumer 端示例

```java
@SpringBootApplication
@EnableConsumerRpc
public class RpcConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(RpcConsumerApplication.class, args);
    }
}

@RestController
public class TestController {
    @RpcReference(serviceVersion = "1.0")
    private TestService testService;

    @GetMapping("/test")
    public int test() {
        return testService.calc(1, 2);
    }
}
```

## Agent 互联服务

- `agent-registry-service`: AgentCard 注册与索引构建（Redis Stack）
- `agent-discovery-service`: 召回/排序/精排（Redis Stack + DashScope）

配置示例见 `agent-registry-service/src/main/resources/application.yml.template` 和 `agent-discovery-service/src/main/resources/application.yml.template`。

## Agent SDK & 示例

- `rpc-agent-spring-boot-starter`: Agent 侧自动注册、心跳、健康检查的 Spring Boot Starter
- AgentCard 配置方式：提供 `AgentCard` Bean；或在 `src/main/resources/agentcard.json` 放置配置文件（Bean 优先）
- `rpc-agent1/2/3`: 三个示例 Agent（旅行规划/旅行信息/娱乐趋势）
