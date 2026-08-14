### 🚀FeignClient Assistant With RequestX:v1.0.0
- 跨模块实现FeignClient导航ApiController功能：FeignClient-ApiController Mutually Navigation
- 跨模块实现ApiController导航FeignClient功能：ApiController-FeignClient Mutually Navigation

### 🚀FeignClient Assistant With RequestX:v2.1.0
适配最新版本的IntelliJ IDEA

### 🐞FeignClient Assistant With RequestX:v3.0.0
[fix] 重大bug修复，修复了由于缓存导致的目标接口动态监测失效的问题

### 🚀FeignClient Assistant With RequestX:v4.0.0
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

### 🚀FeignClient Assistant With RequestX:v4.1.1

适配了最新版IDEA的Light主题，欢迎在IDEA内在线更新至4.1.1版本（三天后上线），或者提前安装离线版体验！
https://github.com/lyflexi/feignx-plugin/releases/tag/v4.1.1

修复了issues:https://github.com/Halfmoonly/feignx-plugin/issues/3


感谢官方工作人员的指引：Natalia Melnikova (JetBrains Marketplace) marketplace@jetbrains.com

感谢社区的帮助与提示：https://intellij-support.jetbrains.com/hc/en-us/community/posts/22814305825042-Why-don-t-pluginIcon-svg-appear-in-Light-theme?page=1#community_comment_22848980293394

感谢@yann Cebron：https://intellij-support.jetbrains.com/hc/en-us/profiles/1283051161-Yann-Cebron

开发社区：https://intellij-support.jetbrains.com/hc/en-us/community/topics/200366979-IntelliJ-IDEA-Open-API-and-Plugin-Development

### 🚀FeignClient Assistant With RequestX:v4.1.3
thanks my friend's pr : https://github.com/Halfmoonly/feignx-plugin/pull/9
1. Adapted bootstrap.properties/bootstrap.yml/bootstrap.yaml
2. Adapted many writtings of path，as @FeignClient(path = "/sys") and @FeignClient(path = "sys") and @FeignClient(path = "sys/")


### 🚀FeignClient Assistant With RequestX:v4.1.5
极少数分布式场景下的nacos需要以下配置，往往会配置在本地的bootstrap.yml/yaml中，而非application.yml/yaml中

1. server.servlet.context-path = /hello
2. spring.mvc.servlet.path = /world

此版本修复了FeignX读取本地bootstrap.yml/yaml中上述配置失效的问题

### 🐞FeignClient Assistant With RequestX:v4.1.6
FeignX 4.1.5 is binary incompatible with IntelliJ IDEA Ultimate IU-193.7288.26 due to the following problem Method not found . This can lead to NoSuchMethodError exception at runtime.

Feignx:v4.1.6 resolve IntelliJ IDEA Ultimate 2019.3.51 compatibility problem.

- remove 1 usage of deprecated API (V1.381)
- Invocation of unresolved method PsiEditorUtil.findEditor(PsiElement)
- Method SearchControllerAction.navigateToControllerCode(...) contains an invokestatic instruction referencing an unresolved method PsiEditorUtil.findEditor(PsiElement).


### 🚀 FeignClient Assistant With RequestX V5.1.0 更新内容
reslove issue #6：https://github.com/Halfmoonly/feignx-plugin/issues/6


1. 我们又更名啦[笑哭]，由FeignX更名为FeignClient Assistant With RequestX
2. 为了方便Vim党，我们支持了url完整路径复制至剪切板（Feign接口和Controller接口均支持）
3. 欢迎Star：https://github.com/Halfmoonly/feignx-plugin

朋友们记得先将zip解压出jar包，再离线安装jar包哟~~：https://github.com/user-attachments/files/19149196/Navigator4URL.OpenFeign.RestController-5.1.0.zip

### 🐞 FeignClient Assistant With RequestX V5.1.1 更新内容
修复分支：hotfix/main-copy-notify

1. 我们修复了一键复制URL功能的消息通知失败的问题，以及偶先空指针的现象

2. 我们优化了Copy-Gutter和Bird-Gutter的展现位置，将其从方法签名处移至RequestMapping等Rest注解处，这样更加符合直觉

3. 我们优化了Copy-Gutter和Bird-Gutter的动态解析，使用户后期在修改方法签名的时候（如添加/**/注释或者添加自定义业务注解的时候），Gutter的位置随着RequestMapping等Rest注解的位置动态生效

4. 我们优化了一键复制URL功能的Copy-Gutter图标设计，更加的优雅


### 🐞 FeignClient Assistant With RequestX V5.1.2 更新内容

1. 我们修复了https://github.com/Halfmoonly/feignx-plugin/issues/11，这曾经是个已经被修复但忘记合并至主分支的修复分支：hotfix/main-fix-bootstrap，见：https://github.com/Halfmoonly/feignx-plugin/issues/8

### 🚀 FeignClient Assistant With RequestX V5.2.1 更新内容
对应分支：feat/main-parallel

默认IO密集型程序核心线程数为`2*N`，并自定义了线程池，优化初始化过程中，构建出全量接口方法对象HttpMappingInfos的速度（ApiControllers和FeignClients）

### 🐞 FeignClient Assistant With RequestX V5.3.1 更新内容
对应分支：main-fix-dead

1. 修复了偶发的卡死现象

### 🐞 FeignClient Assistant With RequestX V5.3.2 更新内容
对应分支：main-fix-dead2

1. 修复了偶发的卡死现象

### 🐞🚀 FeignClient Assistant With RequestX V5.4.0 更新内容
对应分支：hotfix/main-dead

1. feat: 合并了来自开发者的一个特性PR：https://github.com/Halfmoonly/feignx-plugin/pull/14 ,支持Restful注解path中的变量或者静态常量的解析（FeignClient和ApiController），感谢@wdhcr

![restful-path-constant.png](../pics/restful-path-constant.png)

2. fix: 重大bug修复，彻底修复了打开多个IDEA项目时候偶发的卡死现象，我们定位到是线程池的问题并做出了修复与避免。希望反馈的朋友们耐心等待此版本上架

### 🚀 FeignClient Assistant With RequestX v5.5.0更新内容
对应分支feat/main-tab-svg

1. 我们上线了FeignClient类文件和Tab页签的图标替换功能，默认开启，用户可以在IDEA设置面板中关闭，修改配置后记得重启IDEA。
2. 你将体验到全新的UI设计风格，包括URL路径一键复制，希望大家喜欢(❤ ω ❤)

![user-settings.png](../pics/user-settings.png)

### 🐞 FeignClient Assistant With RequestX v5.5.1版本修复如下异常

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
### 🐞 FeignClient Assistant With RequestX v5.5.2版本修复如下异常

修复分支：hotfix/main-fix-tabsvg2

1. 修复了回车键可能导致的方法旁gutter失效的问题

### 🐞 FeignClient Assistant With RequestX v5.5.3更新内容
修复分支：main

1. 修复了二次打开项目的时候,有gutter,但无法跳转的问题

### 🐞 FeignClient Assistant With RequestX v5.5.4更新内容
修复分支：hotfix/main-fix-gitpull

1. 修复了git pull操作变更了psiclass导致的gutter跳转失效的问题

### 🐞 FeignClient Assistant With RequestX v5.5.5更新内容
修复分支：main

1. 修复了异常：invalidated at: see attachment

### 🐞 FeignClient Assistant With RequestX v5.5.6更新内容
修复分支：main

1. 修复了异常：java.lang.Throwable: Smart pointers must not be created during PSI changes

### 🐞 FeignClient Assistant With RequestX v5.5.7更新内容
修复分支：main

1. 修复了异常：PsiInvalidElementAccessException

### 🐞 FeignClient Assistant With RequestX v5.5.8更新内容
修复分支：main

1. 重新设计了UI，修复了图标在Tab页签中无法居左显示的问题。

### 🚀 FeignClient Assistant With RequestX v5.6.0更新内容
更新分支：main

1. 过滤了不必要的psi监听事件消费，提升了psiclass的监听处理性能

### 🐞 FeignClient Assistant With RequestX v5.6.1更新内容
修复分支：hotfix/main-fix-psichange-dumb

1. 当切换git分支的场景下，修复了当前项目psichange事件监听消费处，索引还未更新完成导致的Dumb异常

### 🚀 FeignClient Assistant With RequestX v5.6.2更新内容
优化分支：main

1. 优化了UI

### 🚀 FeignClient Assistant With RequestX v5.6.3.1更新内容
优化分支：feat/main-ui2

1. 优化了ApiController控制器侧的UI

### 🐞 FeignClient Assistant With RequestX v5.6.3.2更新内容
修复分支：hotfix/main-fix-duplicate-TestEntityManagerAutoConfiguration

1. 使用intellij自家判断注解的API，修复了java.lang.Throwable: PersistentFS[connected: true, ownData: com.intellij.openapi.vfs.newvfs.impl.VfsData@675c6da6] returned duplicate file names('TestEntityManagerAutoConfiguration.class', 'TestEntityManagerAutoConfiguration.class') caseSensitive: true SystemInfo.isFileSystemCaseSensitive: false isCaseSensitive(): true SystemInfo.OS: Windows 10.0 wasChildrenLoaded: true in the dir: jar://C:/Users/hasee/.m2/repository/org/springframework/boot/spring-boot-test-autoconfigure/3.0.0/spring-boot-test-autoconfigure-3.0.0.jar!/org/springframework/boot/test/autoconfigure/orm/jpa; 9 children: ["AutoConfigureDataJpa.class"; nameId: 189261; id: 98540 (unknown), "DataJpaTypeExcludeFilter.class"; nameId: 189274; id: 98542 (unknown), "TestEntityManagerAutoConfiguration.class"; nameId: 189302; id: 98544 (unknown), "DataJpaTestContextBootstrapper.class"; nameId: 189323; id: 98547 (unknown), "Te...

### 🚀 FeignClient Assistant With RequestX v5.6.4.0更新内容
优化分支：feat/main-performance

1. 我们发现在大型项目中插件的初始化速度较慢，v5.6.4.0通过排除三方依赖中的libs，仅保留用户文件的扫描，提升了PSI扫描的速度



https://plugins.jetbrains.com/plugin/25604-feignclient-assistant

IDEA内插件市场一键安装最方便哟~~

### 🐞 FeignClient Assistant With RequestX v5.6.4.5更新内容

修复了 issues #21 / #23：

1. 支持 Restful 注解路径中的常量拼接：`@GetMapping(CONST + "/xxx")`、`@PostMapping(CONST)`、`@FeignClient(path = CONST)` 等写法均可正确解析（含嵌套常量，如 常量 = 常量 + "/xxx"）。
2. 复制 URL 时支持拼接 `@RequestParam` query 参数：方法参数带 `@RequestParam("code") Integer code` 时，复制的 Controller/Feign URL 会自动拼入 `?code=`，方便直接粘贴到浏览器/Postman 填充参数。
3. 配套新增 sample 测试用例：`ConstantPathServerController`（server 端）+ `UserClientConstant`/`UserClientFeignConst`（client 端），覆盖常量拼接、嵌套常量、@FeignClient path 常量与 @RequestParam 场景。

同时顺带修复了 issue #17：支持从 `application-{profile}.yml/yaml/properties`、`bootstrap-{profile}.yml` 等 profile 专属配置文件中解析 `server.servlet.context-path` / `spring.mvc.servlet.path`（按 `spring.profiles.active` 解析，maven 占位符场景兜底扫描 profile 文件）。

### 🐞 FeignClient Assistant With RequestX v5.6.4.6更新内容

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

### 🚀 FeignClient Assistant With RequestX v5.6.4.7更新内容

新增 issue #25 的兜底功能：StatusBar 右下角手动刷新。

1. 在 IDEA 右下角 StatusBar 新增一个刷新图标（使用插件图标 `pluginIcon.svg`，按 StatusBar 紧凑尺寸自适应缩放），点击后手动触发一次全量刷新：
   - 基于注解索引重新扫描当前工程下所有 Controller 与 FeignClient，并统计扫描结果；
   - 重启 Daemon 强制 gutter 导航图标重新计算，兜底解决索引/缓存异常导致的跳转失效。
2. 刷新逻辑与 UI 解耦：`com.lyflexi.feignx.refresh.FeignRefreshManager` 独立于 StatusBar 展示层，后续可基于同一入口扩展快捷键 / Action 等触发方式。
3. 刷新在后台线程执行，不阻塞 UI；索引未就绪时会给出提示。
4. 图标 hover 提示为英文：`Refresh all FeignClients and Controllers`。
5. 状态栏图标引用 `icons/pluginIcon.svg`（与 `META-INF/pluginIcon.svg` 同内容的副本）：插件类加载器下直接加载 `/META-INF/pluginIcon.svg` 会显示默认占位图标（咖啡杯），而 `/icons/` 目录与 gutter 图标同源、已验证可正常加载；`META-INF/pluginIcon.svg` 仍保留用于插件市场图标。

### 🚀 FeignClient Assistant With RequestX v5.7.0更新内容

新增独立功能:右侧"feignx assistant endpoints"端点侧边栏。

1. 在 IDEA 右侧新增侧边栏,图标复用 icons/jumpAction_controller.svg,展开后展示工程内全部 SpringMVC 端点与 Feign 端点及其请求路径。
2. 端点分为 SpringBoot / SpringMVC / OpenFeign 三个分组(SpringBoot 排最上),分组下先按类、再按请求路径逐级展开(SpringBoot 分组只展示 @SpringBootApplication 启动类);每个请求前标注 GET / POST / PUT / DELETE(泛化 @RequestMapping 显示为 ALL)。
3. 类节点左侧使用 gutter 图标区分类型:SpringMVC 类使用 icons/jumpAction_controller.svg,OpenFeign 类使用 icons/jumpAction_feign.svg,SpringBoot 启动类使用经典 Spring 图标 icons/spring.svg,无需配色即可一眼区分。
4. 顶部支持:按路径模糊搜索(contains,忽略大小写) + HttpType 请求类型(GET/POST/PUT/DELETE)下拉过滤 + EndpointType(SpringBoot/SpringMVC/OpenFeign)分类下拉过滤,左上角提供刷新按钮,点击后台重新全量扫描;新增"全部展开/全部收起"按钮。
5. 双击类节点可跳转到该类源码,双击请求路径节点可跳转到对应接口方法。
6. 该功能完全独立实现于 com.lyflexi.feignx.toolwindow 包,不修改任何历史 gutter / 状态栏 / 配置代码,仅复用已有公共扫描工具类。

### 🚀 FeignClient Assistant With RequestX v5.7.1更新内容
更新分支：main

1. 端点侧边栏右侧新增请求调试面板：左侧搜索树保持不变，右侧布局分为左右两块。
2. 左块：点击具体请求节点时，自动生成标准 HTTP 脚本模板（支持 GET / POST / PUT / DELETE），并兼容三类参数——`@RequestBody` 请求体（递归解析实体类及其字段，生成带字段的 JSON 模板，支持嵌套对象/集合/枚举/日期类型）、`@RequestParam` 请求参数（拼接为 query 串）、`@PathVariable` 路径参数（替换路径占位符为示例值）。
3. 左块顶部提供基础地址输入框（默认 `http://localhost:8080`）与「执行」按钮，点击后在网络后台线程发送真实 HTTP 请求；脚本基于 IntelliJ HTTP Client 格式，可自由编辑。
4. 右块展示最近一次请求的响应体（状态码 + 响应头 + 响应体 + 耗时），请求在后台线程执行，不阻塞 UI。
5. 新增请求历史功能：每次执行后自动将「请求脚本 + 响应结果」落盘到当前工程 `.idea/feignx-history` 目录（按时间倒序），右块「历史请求」按钮可弹出历史列表（支持输入过滤），选中即可一键回填脚本与响应，方便复用与追溯。
