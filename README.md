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
启动Provider、Consumer需要添加JVM参数
```bash
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/jdk.internal.misc=ALL-UNNAMED
--add-opens java.base/java.nio=ALL-UNNAMED
--add-opens java.base/sun.nio.ch=ALL-UNNAMED
-Dio.netty.tryReflectionSetAccessible=true
```