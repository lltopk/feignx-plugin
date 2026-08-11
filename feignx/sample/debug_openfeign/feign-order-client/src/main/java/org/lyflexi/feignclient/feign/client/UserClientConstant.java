package org.lyflexi.feignclient.feign.client;

import org.lyflexi.cloudfeignapi.User;
import org.lyflexi.cloudfeignapi.UserApiConst;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 复现 Issue #21 #23:feign 方法路径常量拼接
 * 对应 cloud-feign-server 模块的 ConstantPathServerController.list / nested
 * path 前缀 /hello/world 对齐 server 端 context-path(/hello)+mvc(/world)
 *
 * @Author: feignx
 */
@FeignClient(value = "cloud-feign-server", path = "/hello/world", contextId = "userConstant")
public interface UserClientConstant {

    /**
     * Issue #23:方法路径常量拼接 @PostMapping(常量 + "/xxx")
     * 完整路径:/hello/world/constant/list
     */
    @PostMapping(UserApiConst.USER_CONST_PREFIX + "/list")
    User list(@RequestParam("code") Integer code);

    /**
     * Issue #23:嵌套常量(常量 = 常量 + "/xxx") 递归解析
     * 完整路径:/hello/world/constant/list/nested
     */
    @PostMapping(UserApiConst.USER_CONST_LIST + "/nested")
    User nested(@RequestParam("code") Integer code);
}
