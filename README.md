
# 原stub存根项目
- 微服务框架
- -  注：仅适用于中台的后置微服务开发，它需要openport做前置（前置是开放给公网的要有保护机制，openport有很好方案，而stub要开发者自已实现）。openport是网关2的另一个微服务框架，openport可以做中台架构的前后置微服务，因此建议多用openport
- 从网关2源码工程中移出来改了包名，与之前的对照关系是：
  cj.studio.gateway.stub->cj.studio.stub.client
  cj.studio.gateway.socket(从socket中剪出）->cj.studio.stub.service

## 示例工程
- stub目前用的是比较多的，目前应用的大概有50个微服务，开源出来的uc/uaac工程均是stub项目
- stub转openport，多数api兼容，切换代价仅在于接口的注解声明上，其它均不受影响

