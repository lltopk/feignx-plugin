package org.lyflexi.cloudfeignserver.controller;

import org.lyflexi.cloudfeignapi.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对应 feign 侧 UserClientSelfConst(内部字符串变量拼接,Issue #26)
 * 类级 @RequestMapping("/self") + 方法路径,配合模块 context-path(/hello)+mvc(/world) 前缀
 *
 * @Author: feignx
 */
@RestController
@RequestMapping("/self")
public class SelfConstServerController {

    /**
     * 完整路径:/hello/world/self/create
     */
    @PostMapping("/create")
    public User create() {
        return new User(1L, "self-create");
    }

    /**
     * 完整路径:/hello/world/self/createNested
     */
    @PostMapping("/createNested")
    public User createNested() {
        return new User(1L, "self-create-nested");
    }
}
