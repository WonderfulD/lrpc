package space.ruiwang.agent.util;

import cn.hutool.core.util.StrUtil;

/**
 * Redis key helpers for agent data.
 */
public final class AgentKeyBuilder {
    private AgentKeyBuilder() {
    }

    public static String agentKey(String prefix, String agentId) {
        return StrUtil.blankToDefault(prefix, "agent:") + agentId;
    }

    public static String skillKey(String prefix, String agentId, String skillId) {
        return StrUtil.blankToDefault(prefix, "skill:") + agentId + ":" + skillId;
    }
}
