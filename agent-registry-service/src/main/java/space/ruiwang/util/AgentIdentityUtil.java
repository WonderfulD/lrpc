package space.ruiwang.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import space.ruiwang.domain.agent.AgentCard;
import space.ruiwang.domain.agent.AgentEndpoint;
import space.ruiwang.domain.agent.AgentProvider;

public final class AgentIdentityUtil {
    private AgentIdentityUtil() {
    }

    public static String identityHash(AgentCard agent) {
        if (agent == null) {
            return "";
        }
        AgentProvider provider = agent.getProvider();
        AgentEndpoint endpoint = agent.getEndpoint();
        String key = String.join("|",
                StrUtil.nullToEmpty(provider == null ? null : provider.getOrganization()),
                StrUtil.nullToEmpty(provider == null ? null : provider.getUrl()),
                StrUtil.nullToEmpty(agent.getName()),
                StrUtil.nullToEmpty(endpoint == null ? null : endpoint.getUrl()));
        if (StrUtil.isBlank(key)) {
            return "";
        }
        return DigestUtil.sha256Hex(key);
    }
}
