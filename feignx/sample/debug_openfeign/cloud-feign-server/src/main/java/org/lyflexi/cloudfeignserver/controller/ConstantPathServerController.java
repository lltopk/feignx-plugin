package org.lyflexi.cloudfeignserver.controller;

import org.lyflexi.cloudfeignapi.User;
import org.lyflexi.cloudfeignapi.UserApiConst;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 复现 Issue #21 #23:映射路径中的常量拼接
 * 注意:类级无 @RequestMapping,方法路径为全路径,配合模块的 context-path(/hello)+mvc(/world) 前缀
 *
 * @Author: feignx
 */
@RestController
public class ConstantPathServerController {

    /**
     * Issue #23:@PostMapping(常量 + "/xxx") 拼接路径
     * Issue #21:方法带 @RequestParam("code") 参数,复制URL时会拼入 query 串
     * 完整路径:/hello/world/constant/list?code=
     */
    @PostMapping(UserApiConst.USER_CONST_PREFIX + "/list")
    public User list(@RequestParam("code") Integer code) {
        return new User(code.longValue(), "user");
    }

    /**
     * Issue #23:嵌套常量(常量 = 常量 + "/xxx") 递归解析
     * 完整路径:/hello/world/constant/list/nested?code=
     */
    @PostMapping(UserApiConst.USER_CONST_LIST + "/nested")
    public User nested(@RequestParam("code") Integer code) {
        return new User(code.longValue(), "user");
    }

    /**
     * 对应 feign 侧 @FeignClient(path = 常量) 的场景
     * 完整路径:/hello/world/feignconst/get?code=
     */
    @PostMapping("/feignconst/get")
    public User get(@RequestParam("code") Integer code) {
        return new User(code.longValue(), "user");
    }
}
