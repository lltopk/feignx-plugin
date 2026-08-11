### 🚀FeignClient Assistant:v1.0.0
- 跨模块实现FeignClient导航ApiController功能：FeignClient-ApiController Mutually Navigation
- 跨模块实现ApiController导航FeignClient功能：ApiController-FeignClient Mutually Navigation

### 🚀FeignClient Assistant:v2.1.0
适配最新版本的IntelliJ IDEA

### 🐞FeignClient Assistant:v3.0.0
[fix] 重大bug修复，修复了由于缓存导致的目标接口动态监测失效的问题

### 🚀FeignClient Assistant:v4.0.0
[feat] 重大功能更新，server端适配了springboot配置文件application.properties/application.yml/application.yaml的解析机制：

支持以下配置的解析
1. tomcat配置属性：server.servlet.context-path
2. springmvc配置属性：spring.mvc.servlet.path

![DispatcherServlet.png](../pics/DispatcherServlet.png)

在 Spring Boot 出现之前，Dispatcher Servlet 是在 web.xml 文件中声明的，如下图
```xml
<web-app>
   <servlet>
         <servlet-name>example</servlet-name> 
        <servlet class> 
             org.springframework.web.servlet.DispatcherServlet 
        </servlet-class> 
        <load-on-startup>1</load -on-startup> 
    </servlet>
   <servlet-mapping>
        <servlet-name>test</servlet-name> 
        <url-pattern>*.test</url-pattern> 
   </servlet-mapping>
 </web-app>
```

这个DispatcherServlet是实际的Servlet，它继承自基类HttpServlet。

在 Spring Boot 出现之后，spring-boot-starter-web starter 自动装配机制将DispatcherServlet默认配置为 URL 模式“/”。

但是，如果需要，我们可以使用自定义 URL 模式。application.properties文件中如下
```properties
server.servlet.context-path=/hello
spring.mvc.servlet.path=/world
```

通过上面的配置，DispatcherServlet被配置为处理 URL 模式/world，并且springboot根上下文路径将是/hello。因此，DispatcherServlet监听http://ip/port/hello/world，，as prefix path by @FeignClient，the sample is below
```java
@FeignClient(path = "/hello/world",value = "cloud-feign-server", contextId = "user", configuration = UserConfiguration.class)
public interface UserClient {

    @GetMapping(value = "/user/get/{id}")
    User getUserById(@PathVariable("id") Long id);
}
```


yml/yaml配置同上。

### 🚀FeignClient Assistant:v4.1.1

适配了最新版IDEA的Light主题，欢迎在IDEA内在线更新至4.1.1版本（三天后上线），或者提前安装离线版体验！
https://github.com/lyflexi/feignx-plugin/releases/tag/v4.1.1

修复了issues:https://github.com/Halfmoonly/feignx-plugin/issues/3


感谢官方工作人员的指引：Natalia Melnikova (JetBrains Marketplace) marketplace@jetbrains.com

感谢社区的帮助与提示：https://intellij-support.jetbrains.com/hc/en-us/community/posts/22814305825042-Why-don-t-pluginIcon-svg-appear-in-Light-theme?page=1#community_comment_22848980293394

感谢@yann Cebron：https://intellij-support.jetbrains.com/hc/en-us/profiles/1283051161-Yann-Cebron

开发社区：https://intellij-support.jetbrains.com/hc/en-us/community/topics/200366979-IntelliJ-IDEA-Open-API-and-Plugin-Development

### 🚀FeignClient Assistant:v4.1.3
thanks my friend's pr : https://github.com/Halfmoonly/feignx-plugin/pull/9
1. Adapted bootstrap.properties/bootstrap.yml/bootstrap.yaml
2. Adapted many writtings of path，as @FeignClient(path = "/sys") and @FeignClient(path = "sys") and @FeignClient(path = "sys/")


### 🚀FeignClient Assistant:v4.1.5
极少数分布式场景下的nacos需要以下配置，往往会配置在本地的bootstrap.yml/yaml中，而非application.yml/yaml中

1. server.servlet.context-path = /hello
2. spring.mvc.servlet.path = /world

此版本修复了FeignX读取本地bootstrap.yml/yaml中上述配置失效的问题

### 🐞FeignClient Assistant:v4.1.6
FeignX 4.1.5 is binary incompatible with IntelliJ IDEA Ultimate IU-193.7288.26 due to the following problem Method not found . This can lead to NoSuchMethodError exception at runtime.

Feignx:v4.1.6 resolve IntelliJ IDEA Ultimate 2019.3.51 compatibility problem.

- remove 1 usage of deprecated API (V1.381)
- Invocation of unresolved method PsiEditorUtil.findEditor(PsiElement)
- Method SearchControllerAction.navigateToControllerCode(...) contains an invokestatic instruction referencing an unresolved method PsiEditorUtil.findEditor(PsiElement).

--- 
### 🚀 FeignClient Assistant:v4.1.8
v4.1.8发布——对应分支main-fix-cachev2

reslove issue #10：https://github.com/Halfmoonly/feignx-plugin/issues/10

        1. 我们更名啦，由FeignX更名为FeignClient Assistant
        2. 极致提升性能，引入CacheManager，管理双边缓存FeignInterface和ApiController
        3. 欢迎Star：https://github.com/Halfmoonly/feignx-plugin

[feignx-4.1.8.zip](https://github.com/user-attachments/files/19140074/feignx-4.1.8.zip)


### 🐞 FeignClient Assistant:v4.1.9
v4.1.9发布——对应分支main-fix-cachev3

我们优化了双边缓存更新策略（Optimize cache update policies）

https://github.com/user-attachments/files/19148594/OpenFeign.Assistant-4.1.9.zip


### 🚀 FeignClient Assistant V5.1.0 更新内容
reslove issue #6：https://github.com/Halfmoonly/feignx-plugin/issues/6


1. 我们又更名啦[笑哭]，由FeignX更名为FeignClient Assistant
2. 为了方便Vim党，我们支持了url完整路径复制至剪切板（Feign接口和Controller接口均支持）
3. 欢迎Star：https://github.com/Halfmoonly/feignx-plugin

朋友们记得先将zip解压出jar包，再离线安装jar包哟~~：https://github.com/user-attachments/files/19149196/Navigator4URL.OpenFeign.RestController-5.1.0.zip

### 🐞 FeignClient Assistant V5.1.1 更新内容
修复分支：hotfix/main-copy-notify

1. 我们修复了一键复制URL功能的消息通知失败的问题，以及偶先空指针的现象

2. 我们优化了Copy-Gutter和Bird-Gutter的展现位置，将其从方法签名处移至RequestMapping等Rest注解处，这样更加符合直觉

3. 我们优化了Copy-Gutter和Bird-Gutter的动态解析，使用户后期在修改方法签名的时候（如添加/**/注释或者添加自定义业务注解的时候），Gutter的位置随着RequestMapping等Rest注解的位置动态生效

4. 我们优化了一键复制URL功能的Copy-Gutter图标设计，更加的优雅


### 🐞 FeignClient Assistant V5.1.2 更新内容

1. 我们修复了https://github.com/Halfmoonly/feignx-plugin/issues/11，这曾经是个已经被修复但忘记合并至主分支的修复分支：hotfix/main-fix-bootstrap，见：https://github.com/Halfmoonly/feignx-plugin/issues/8


### 🐞 FeignClient Assistant V5.2.0 更新内容
本次对应修复/缓存优化/多线程优化/API优化的分支：main-fix-cachev4

1. 我们优化了双边缓存的更新机制，同时重构了缓存框架，大大提升了插件性能
2. 我们优化了用户打注释/***/的时候，由于psiMethod丢失，可能导致的空指针异常
3. 我们使用了IntelliJ的类快速索引缓存系统PsiShortNamesCache，狠狠加速了原来的手写磁盘递归扫描Class（allJavaFileClass）
4. 我们使用了Java线程池，加速了初始化过程中，构建出全量接口方法对象HttpMappingInfos的速度（ApiControllers和FeignClients）
5. 我们使用了IntelliJ的带缓存的注解判断方法psiMethod.hasAnnotation，加速了类型判断（ApiController和FeignClient）

重构的双边缓存架构：

![Bilateral-cache.png](../pics/Bilateral-cache.png)

有匪君子，如切如磋，如琢如磨--2025/03/15 凌晨两点

### 🚀 FeignClient Assistant V5.2.1 更新内容
对应分支：feat/main-parallel

默认IO密集型程序核心线程数为`2*N`，并自定义了线程池，优化初始化过程中，构建出全量接口方法对象HttpMappingInfos的速度（ApiControllers和FeignClients）

### 🚀 FeignClient Assistant V5.3.0 更新内容
对应分支：feat/main-cache

我们额外自定义了项目初始化PsiClass缓存管理器InitialPsiClassCacheManager，将原先的两次全盘allJavaFileClass扫描降低为1次，狠狠加速了原来手写的磁盘递归扫描

### 🐞 FeignClient Assistant V5.3.1 更新内容
对应分支：main-fix-dead

1. 修复了偶发的卡死现象

### 🐞 FeignClient Assistant V5.3.2 更新内容
对应分支：main-fix-dead2

1. 修复了偶发的卡死现象

### 🐞🚀 FeignClient Assistant V5.4.0 更新内容
对应分支：hotfix/main-dead

1. feat: 合并了来自开发者的一个特性PR：https://github.com/Halfmoonly/feignx-plugin/pull/14 ,支持Restful注解path中的变量或者静态常量的解析（FeignClient和ApiController），感谢@wdhcr

![restful-path-constant.png](../pics/restful-path-constant.png)

2. fix: 重大bug修复，彻底修复了打开多个IDEA项目时候偶发的卡死现象，我们定位到是线程池的问题并做出了修复与避免。希望反馈的朋友们耐心等待此版本上架

### 🚀 FeignClient Assistant v5.5.0更新内容
对应分支feat/main-tab-svg

1. 我们上线了FeignClient类文件和Tab页签的图标替换功能，默认开启，用户可以在IDEA设置面板中关闭，修改配置后记得重启IDEA。
2. 你将体验到全新的UI设计风格，包括URL路径一键复制，希望大家喜欢(❤ ω ❤)

![user-settings.png](../pics/user-settings.png)

### 🐞 FeignClient Assistant v5.5.1版本修复如下异常

修复分支：hotfix/main-fix-tabsvg

定位到是由于v5.5.0中上线的类文件的图标替换功能导致 PSI 元素失效，影响了原先正常的主流程解析逻辑

1. 文件被修改（例如用户编辑代码）。
2. PSI 元素的访问必须在 读操作（Read Action） 或 事件分发线程（EDT） 中进行。
3. 如果在后台线程（非 EDT）直接访问 PSI 元素，可能导致元素失效。
4. 未检查元素有效性等
```
com.intellij.psi.PsiInvalidElementAccessException: Element: class com.intellij.psi.impl.source.PsiClassImpl #JAVA 
invalidated at: see attachment
    at com.intellij.psi.impl.source.SubstrateRef$1.getNode(SubstrateRef.java:43)
    at com.intellij.extapi.psi.StubBasedPsiElementBase.getNode(StubBasedPsiElementBase.java:133)
    at com.intellij.psi.impl.source.PsiClassImpl.getNode(PsiClassImpl.java:102)
    at com.intellij.psi.impl.source.PsiClassImpl.getNode(PsiClassImpl.java:36)
    at com.intellij.extapi.psi.StubBasedPsiElementBase.getStubOrPsiChild(StubBasedPsiElementBase.java:36
    at com.intellij.extapi.psi.StubBasedPsiElementBase.getRequiredStubOrPsiChild(StubBasedPsiElementBase.java:375)
    at com.intellij.psi.impl.source.PsiClassImpl.getModifierList(PsiClassImpl.java:170)
    at com.intellij.psi.PsiJvmConversionHelper.hasListAnnotation(PsiJvmConversionHelper.java:57)
    at com.intellij.psi.PsiModifierListOwner.hasAnnotation(PsiModifierListOwner.java:45)
    at com.intellij.psi.PsiJvmModifiersOwner.hasAnnotation(PsiJvmModifiersOwner.java:32)
    at com.lyflexi.feignx.utils.AnnotationParserUtils.isFeignInterface(AnnotationParserUtils.java:101)
    at com.lyflexi.feignx.utils.FeignClassScanUtils.feignsOfPsiClass(FeignClassScanUtils.java:123)
    at com.lyflexi.feignx.utils.FeignClassScanUtils.scanFeignInterfaces(FeignClassScanUtils.java:107)
    at com.lyflexi.feignx.provider.C2FLineMarkerProvider.collectNavigationMarkers(Controller2FeignLineMarkerProvider.java:43)
...
```
### 🐞 FeignClient Assistant v5.5.2版本修复如下异常

修复分支：hotfix/main-fix-tabsvg2

1. 修复了回车键可能导致的方法旁gutter失效的问题

### 🐞 FeignClient Assistant v5.5.3更新内容
修复分支：main

1. 修复了二次打开项目的时候,有gutter,但无法跳转的问题

### 🐞 FeignClient Assistant v5.5.4更新内容
修复分支：hotfix/main-fix-gitpull

1. 修复了git pull操作变更了psiclass导致的gutter跳转失效的问题

### 🐞 FeignClient Assistant v5.5.5更新内容
修复分支：main

1. 修复了异常：invalidated at: see attachment

### 🐞 FeignClient Assistant v5.5.6更新内容
修复分支：main

1. 修复了异常：java.lang.Throwable: Smart pointers must not be created during PSI changes

### 🐞 FeignClient Assistant v5.5.7更新内容
修复分支：main

1. 修复了异常：PsiInvalidElementAccessException

### 🐞 FeignClient Assistant v5.5.8更新内容
修复分支：main

1. 重新设计了UI，修复了图标在Tab页签中无法居左显示的问题。

### 🚀 FeignClient Assistant v5.6.0更新内容
更新分支：main

1. 过滤了不必要的psi监听事件消费，提升了psiclass的监听处理性能

### 🐞 FeignClient Assistant v5.6.1更新内容
修复分支：hotfix/main-fix-psichange-dumb

1. 当切换git分支的场景下，修复了当前项目psichange事件监听消费处，索引还未更新完成导致的Dumb异常

### 🚀 FeignClient Assistant v5.6.2更新内容
优化分支：main

1. 优化了UI

### 🚀 FeignClient Assistant v5.6.3.1更新内容
优化分支：feat/main-ui2

1. 优化了ApiController控制器侧的UI

### 🐞 FeignClient Assistant v5.6.3.2更新内容
修复分支：hotfix/main-fix-duplicate-TestEntityManagerAutoConfiguration

1. 使用intellij自家判断注解的API，修复了java.lang.Throwable: PersistentFS[connected: true, ownData: com.intellij.openapi.vfs.newvfs.impl.VfsData@675c6da6] returned duplicate file names('TestEntityManagerAutoConfiguration.class', 'TestEntityManagerAutoConfiguration.class') caseSensitive: true SystemInfo.isFileSystemCaseSensitive: false isCaseSensitive(): true SystemInfo.OS: Windows 10.0 wasChildrenLoaded: true in the dir: jar://C:/Users/hasee/.m2/repository/org/springframework/boot/spring-boot-test-autoconfigure/3.0.0/spring-boot-test-autoconfigure-3.0.0.jar!/org/springframework/boot/test/autoconfigure/orm/jpa; 9 children: ["AutoConfigureDataJpa.class"; nameId: 189261; id: 98540 (unknown), "DataJpaTypeExcludeFilter.class"; nameId: 189274; id: 98542 (unknown), "TestEntityManagerAutoConfiguration.class"; nameId: 189302; id: 98544 (unknown), "DataJpaTestContextBootstrapper.class"; nameId: 189323; id: 98547 (unknown), "Te...

### 🚀 FeignClient Assistant v5.6.4.0更新内容
优化分支：feat/main-performance

1. 我们发现在大型项目中插件的初始化速度较慢，v5.6.4.0通过排除三方依赖中的libs，仅保留用户文件的扫描，提升了PSI扫描的速度



https://plugins.jetbrains.com/plugin/25604-feignclient-assistant

IDEA内插件市场一键安装最方便哟~~

### 🐞 FeignClient Assistant v5.6.4.5更新内容

修复了 issues #21 / #23：

1. 支持 Restful 注解路径中的常量拼接：`@GetMapping(CONST + "/xxx")`、`@PostMapping(CONST)`、`@FeignClient(path = CONST)` 等写法均可正确解析（含嵌套常量，如 常量 = 常量 + "/xxx"）。
2. 复制 URL 时支持拼接 `@RequestParam` query 参数：方法参数带 `@RequestParam("code") Integer code` 时，复制的 Controller/Feign URL 会自动拼入 `?code=`，方便直接粘贴到浏览器/Postman 填充参数。
3. 配套新增 sample 测试用例：`ConstantPathServerController`（server 端）+ `UserClientConstant`/`UserClientFeignConst`（client 端），覆盖常量拼接、嵌套常量、@FeignClient path 常量与 @RequestParam 场景。

同时顺带修复了 issue #17：支持从 `application-{profile}.yml/yaml/properties`、`bootstrap-{profile}.yml` 等 profile 专属配置文件中解析 `server.servlet.context-path` / `spring.mvc.servlet.path`（按 `spring.profiles.active` 解析，maven 占位符场景兜底扫描 profile 文件）。

### 🐞 FeignClient Assistant v5.6.4.6更新内容

修复了 issue #26：feign 接口**内部**定义的字符串变量拼接跳转不了，如：

```java
@FeignClient(value = "market-api-trade")
public interface TradeInnerOrderFeignApi {
    String serverName = "/zzgg/ggpg/server/innerorder";
    @PostMapping(value = serverName + "/createsnorder", produces = "application/json")
    Result<JSONObject> managementAddShopPayOrderRequest(...);
}
```

1. 与 #21/#23（常量来自外部常量类）不同，本次支持常量定义在当前 feign 接口自身（接口字段隐式 `public static final`），方法路径 `变量 + "/xxx"`、嵌套常量 `常量 = 常量 + "/xxx"` 均可正确解析并与 controller 双向跳转。
2. 常量解析进一步增强：支持括号包裹的拼接表达式，如 `@GetMapping((CONST) + "/xxx")`。
3. 配套新增 sample 测试用例：`UserClientSelfConst`（feign 侧内部变量拼接 + 嵌套常量）+ `SelfConstServerController`（server 侧）。
