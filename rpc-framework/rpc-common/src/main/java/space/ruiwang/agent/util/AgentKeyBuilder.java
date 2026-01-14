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

    public static String agentAliveKey(String prefix, String agentId) {
        return StrUtil.blankToDefault(prefix, "agent:") + "alive:" + agentId;
    }

    public static String agentIdsKey(String prefix) {
        return StrUtil.blankToDefault(prefix, "agent:") + "ids";
    }

    public static String agentHealthFailKey(String prefix, String agentId) {
        return StrUtil.blankToDefault(prefix, "agent:") + "health:fail:" + agentId;
    }

    public static String agentInactiveKey(String prefix) {
        return StrUtil.blankToDefault(prefix, "agent:") + "inactive";
    }

    public static String agentIdentityKey(String prefix, String identityHash) {
        return StrUtil.blankToDefault(prefix, "agent:") + "identity:" + identityHash;
    }
}
