package space.ruiwang.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import space.ruiwang.annotation.RpcReference;
import space.ruiwang.service.TestService;


@RestController
public class TestController {

    @RpcReference
    private TestService testService;

    @RequestMapping("test/{a}/{b}")
    public String test(@PathVariable Integer a, @PathVariable Integer b) {
        return testService.calc(a, b);
    }

}
