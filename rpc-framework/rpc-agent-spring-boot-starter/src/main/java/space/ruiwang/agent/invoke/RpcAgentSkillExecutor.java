package space.ruiwang.agent.invoke;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.agent.LlmSkill;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeResponse;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeResult;
import space.ruiwang.domain.agent.invoke.AgentSkillInvoker;

@Slf4j
public class RpcAgentSkillExecutor {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    private final AgentSkillInvoker skillInvoker;
    private final ExecutorService executor;

    public RpcAgentSkillExecutor(AgentSkillInvoker skillInvoker) {
        this.skillInvoker = skillInvoker;
        this.executor = Executors.newFixedThreadPool(8);
    }

    public List<AgentSkillInvokeResult> executeParallel(List<LlmSkill> skills, String query, Duration timeout) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }
        Duration resolved = resolveTimeout(timeout);
        List<CompletableFuture<AgentSkillInvokeResult>> futures = new ArrayList<>();
        for (LlmSkill skill : skills) {
            futures.add(CompletableFuture.supplyAsync(() -> execute(skill, query, resolved), executor));
        }
        List<AgentSkillInvokeResult> results = new ArrayList<>();
        for (CompletableFuture<AgentSkillInvokeResult> future : futures) {
            try {
                AgentSkillInvokeResult result =
                        future.get(resolved.toMillis(), TimeUnit.MILLISECONDS);
                if (result != null) {
                    results.add(result);
                }
            } catch (Exception e) {
                log.warn("Skill execution failed", e);
                future.cancel(true);
            }
        }
        return results;
    }

    public AgentSkillInvokeResult execute(LlmSkill skill, String query, Duration timeout) {
        if (skill == null) {
            throw new IllegalArgumentException("Skill is required");
        }
        Duration resolved = resolveTimeout(timeout);
        AgentSkillInvokeRequest request = new AgentSkillInvokeRequest();
        request.setSkillId(skill.getSkillId());
        request.setInput(query);
        request.setQuery(query);

        AgentSkillInvokeResponse response = skillInvoker.invoke(skill, request, resolved);
        return toResult(skill, response);
    }

    private AgentSkillInvokeResult toResult(LlmSkill skill, AgentSkillInvokeResponse response) {
        if (response == null) {
            return null;
        }
        AgentSkillInvokeResult result = new AgentSkillInvokeResult();
        result.setAgentId(skill.getAgentId());
        result.setSkillId(response.getSkillId());
        result.setScore(skill.getScore());
        result.setResult(response.getResult());
        return result;
    }

    private Duration resolveTimeout(Duration timeout) {
        if (timeout != null) {
            return timeout;
        }
        return DEFAULT_TIMEOUT;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
