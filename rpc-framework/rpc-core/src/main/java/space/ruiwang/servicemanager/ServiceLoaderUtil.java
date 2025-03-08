package space.ruiwang.servicemanager;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.utils.redisops.impl.RedissonOps;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-07
 */
@Component
public class ServiceLoaderUtil {
    @Resource
    private RedissonOps redissonOps;
    public List<ServiceRegisterDO> loadService(String serviceKey) {
        List<ServiceRegisterDO> result;
        String serviceListStr = redissonOps.get(serviceKey);
        if (StrUtil.isEmpty(serviceListStr)) {
            // 当前key没有任何服务实例
            result = new ArrayList<>();
        } else {
            // 当前key已有服务实例
            // 反序列化
            result = JSONUtil.toBean(serviceListStr, new TypeReference<>() { }, false);
        }
        return result;
    }
}
