package org.lyflexi.feignclient.feign.client;

import org.lyflexi.cloudfeignapi.User;
import org.lyflexi.cloudfeignapi.UserApiConst;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 复现 Issue #21:feign 使用 @FeignClient(path = 常量)
 * 对应 cloud-feign-server 模块的 ConstantPathServerController.get
 *
 * @Author: feignx
 */
@FeignClient(value = "cloud-feign-server", path = UserApiConst.USER_FEIGN_CONST_PATH, contextId = "userFeignConst")
public interface UserClientFeignConst {

    @PostMapping("/get")
    User get(@RequestParam("code") Integer code);
}
