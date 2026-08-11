package org.lyflexi.feignclient.feign.client;

import org.lyflexi.cloudfeignapi.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 复现 Issue #26:feign 接口内部字符串变量拼接
 * 与 #23 的区别:字符串变量定义在当前 feign 接口中(接口字段隐式 public static final),
 * 而非外部常量类;方法路径通过 变量 + "/xxx" 拼接,如 @PostMapping(value = serverName + "/createsnorder")
 * 对应 cloud-feign-server 模块的 SelfConstServerController
 *
 * @Author: feignx
 */
@FeignClient(value = "cloud-feign-server", contextId = "userSelfConst")
public interface UserClientSelfConst {

    // 当前 feign 接口内的字符串变量(含 server 端前缀,对齐现有 @FeignClient(path = "/hello/world/...") 写法)
    String SELF_SERVER_NAME = "/hello/world/self";

    // 嵌套常量:常量 = 常量 + "/xxx"
    String SELF_CREATE_PATH = SELF_SERVER_NAME + "/createNested";

    /**
     * Issue #26:变量 + "/xxx" 拼接
     * 完整路径:/hello/world/self/create
     */
    @PostMapping(value = SELF_SERVER_NAME + "/create", produces = "application/json")
    User create();

    /**
     * Issue #26:嵌套常量(常量 = 常量 + "/xxx")
     * 完整路径:/hello/world/self/createNested
     */
    @PostMapping(value = SELF_CREATE_PATH)
    User createNested();
}
