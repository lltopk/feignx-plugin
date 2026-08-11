package org.lyflexi.cloudfeignapi;

public interface UserApiConst {


    String USER_CLIENT_BASE = "/hello/world";

    String USER_CLIENT_PARALLEL_SCAN9_ID = "/user/parallelScan9/{id}";

    // Issue #21 #23:常量拼接路径演示,如 @PostMapping(USER_CONST_PREFIX + "/list")
    // 方法路径前缀(不含 server 端 context-path/mvc 前缀,由模块配置解析补充)
    String USER_CONST_PREFIX = "/constant";

    // Issue #23:嵌套常量(常量 = 常量 + "/xxx"),验证递归解析
    String USER_CONST_LIST = USER_CONST_PREFIX + "/list";

    // Issue #21:feign 使用 @FeignClient(path = 常量) 演示
    // 含 server 端前缀,对齐现有 @FeignClient(path = "/hello/world/...") 写法
    String USER_FEIGN_CONST_PATH = "/hello/world/feignconst";
}
