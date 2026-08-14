package org.lyflexi.feignclient.feign.client;

import org.lyflexi.cloudfeignapi.User;
import org.lyflexi.feignclient.feign.config.UserConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @Author: lyflexi
 * @project: debuginfo_jdkToFramework
 * @Date: 2024/7/27 14:13
 */
// http://localhost:9000/consumer/feign/user/get/1
@FeignClient(path = "/hello/world/user",value = "cloud-feign-server", contextId = "user", configuration = UserConfiguration.class)
public interface UserClient5 {


    @DeleteMapping(value = "/get/{id}")
    /**
     *
     */
    User getUserById(@PathVariable("id") Long id);

    @GetMapping(value = "/update2/{id}")
    User update(@PathVariable("id") Long id);

    @GetMapping(value = "/del/{id}")
    User del(@PathVariable("id") Long id);

    @GetMapping(value = "/getfather/{id}")

    User getfather(@PathVariable("id") Long id);

    @GetMapping(value = "/getmather/{id}")

    User getmather(@PathVariable("id") Long id);

    @Deprecated
    @GetMapping(value = "/clipboard/{id}")

    User clipboard(@PathVariable("id") Long id);

    @GetMapping(value = "/clipboard2/{id}")
    User clipboard2(@PathVariable("id") Long id);

    @GetMapping(value = "/clipboard3/{id}")
    User clipboard3(@PathVariable("id") Long id);

    @GetMapping(value = "/clipboard4/{id}")
    User clipboard4(@PathVariable("id") Long id);

    @GetMapping(value = "/optimizedCache/{id}")
    User optimizedCache(@PathVariable("id") Long id);

    @GetMapping(value = "/optimizedCache2/{id}")
    User optimizedCache2(@PathVariable("id") Long id);

    @GetMapping(value = "/optimizedCache3/{id}")
    User optimizedCache3(@PathVariable("id") Long id);

    @GetMapping(value = "/parallelScan/{id}")
    User parallelScan(@PathVariable("id") Long id);

    @GetMapping(value = "/parallelScan2/{id}")
    User parallelScan2(@PathVariable("id") Long id);

    @GetMapping(value = "/parallelScan3/{id}")
    User parallelScan3(@PathVariable("id") Long id);

    @GetMapping(value = "/parallelScan4/{id}")
    User parallelScan4(@PathVariable("id") Long id);

    @GetMapping(value = "/parallelScan5/{id}")
    User parallelScan5(@PathVariable("id") Long id);

    @GetMapping(value = "/parallelScan5/{id}")
    public void test();

    @GetMapping(value = "/parallelScan5/{id}")
    public void tets();

    @GetMapping(value = "/parallelScan7/{id}")
    User parallelScan7(@PathVariable("id") Long id);

    @GetMapping(value = "/parallelScan811/{id}")
    User parallelScan8(@PathVariable("id") Long id);

    @GetMapping(value = "/parallelScan811/{id}")
    User parallelScan811(@PathVariable("id") Long id);

    @GetMapping(value = "/parallelScan110/{id}")
    User parallelScan10(@PathVariable("id") Long id);

    @GetMapping(value = "/parallelScan11/{id}")
    User parallelScan11(@PathVariable("id") Long id);


    @GetMapping(value = "/parallelScan11/{id}")
    User parallelScan111(@PathVariable("id") Long id);

    @GetMapping(value = "/parallelScan12/{id}")
    User parallelScan12(@PathVariable("id") Long id);

}