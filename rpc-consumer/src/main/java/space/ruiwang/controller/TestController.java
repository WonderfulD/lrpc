package space.ruiwang.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import space.ruiwang.annotation.RpcReference;
import space.ruiwang.constants.FaultTolerantStrategies;
import space.ruiwang.service.TestService;
import space.ruiwang.utils.AjaxResult;


@RestController
@RequestMapping("/test")
public class TestController {

    @RpcReference(tolerant = FaultTolerantStrategies.FAIL_OVER)
    private TestService testService;

    @GetMapping("/calc")
    public AjaxResult test(Integer a, Integer b) {
        try {
            String result = testService.calc(a, b);
            return AjaxResult.success(result);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

}
