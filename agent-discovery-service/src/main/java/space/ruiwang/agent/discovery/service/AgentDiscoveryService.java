package space.ruiwang.agent.discovery.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;
import space.ruiwang.agent.dashscope.DashScopeClient;
import space.ruiwang.agent.dashscope.DashScopeMessage;
import space.ruiwang.agent.discovery.config.DiscoveryProperties;
import space.ruiwang.agent.discovery.config.RedisStackProperties;
import space.ruiwang.agent.util.AgentKeyBuilder;
import space.ruiwang.agent.util.VectorUtils;
import space.ruiwang.domain.agent.AgentCard;
import space.ruiwang.domain.agent.ExpandedSkill;
import space.ruiwang.domain.agent.LlmSkill;
import space.ruiwang.domain.agent.SkillCard;
import space.ruiwang.domain.agent.document.AgentCardDocument;
import space.ruiwang.domain.agent.document.SkillDocument;
import space.ruiwang.domain.agent.dto.AgentDiscoverRequest;
import space.ruiwang.domain.agent.dto.AgentDiscoverResponse;

@Service
public class AgentDiscoveryService {
    private final JedisPooled jedis;
    private final DashScopeClient dashScopeClient;
    private final RedisStackProperties redisProperties;
    private final DiscoveryProperties discoveryProperties;

    public AgentDiscoveryService(JedisPooled jedis,
                                 DashScopeClient dashScopeClient,
                                 RedisStackProperties redisProperties,
                                 DiscoveryProperties discoveryProperties) {
        this.jedis = jedis;
        this.dashScopeClient = dashScopeClient;
        this.redisProperties = redisProperties;
        this.discoveryProperties = discoveryProperties;
    }

    public AgentDiscoverResponse discover(AgentDiscoverRequest request) {
        if (request == null || StrUtil.isBlank(request.getQuery())) {
            throw new IllegalArgumentException("Discovery query is required");
        }
        String query = request.getQuery().trim();
        int topK = request.getTopK() == null ? discoveryProperties.getFinalTopK() : request.getTopK();
        if (topK <= 0) {
            topK = discoveryProperties.getFinalTopK();
        }

        List<Float> queryEmbedding = dashScopeClient.embed(query);
        Set<String> keywordAgents = recallAgentsByKeyword(query, discoveryProperties.getAgentRecallTopK());
        Set<String> vectorAgents = recallAgentsByVector(queryEmbedding, discoveryProperties.getAgentRecallTopK());
        Set<String> agentIds = union(keywordAgents, vectorAgents);

        List<AgentCardDocument> agentDocs = fetchAgents(agentIds, request.getMaxAgents());
        List<RankedSkill> candidates = expandSkillsFromAgents(agentDocs, keywordAgents, vectorAgents);

        if (agentIds.size() < discoveryProperties.getMinAgentRecall()) {
            candidates.addAll(recallSkillsByFallback(query, queryEmbedding, agentIds));
        }

        candidates = dedupeCandidates(candidates);
        List<RankedSkill> coarseRanked = coarseRank(candidates, query, discoveryProperties.getCoarseTopK());
        List<RankedSkill> fineRanked = fineRank(coarseRanked, query, discoveryProperties.getFineTopK());

        List<RankedSkill> selected = applyDiversity(fineRanked, topK, discoveryProperties.getMaxSkillsPerAgent());
        List<LlmSkill> skills = selected.stream()
                .map(this::toLlmSkill)
                .collect(Collectors.toList());

        Set<String> selectedAgentIds = selected.stream()
                .map(r -> r.skill.getAgentId())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        List<AgentCard> agents = fetchAgentCards(selectedAgentIds);

        AgentDiscoverResponse response = new AgentDiscoverResponse();
        response.setSkills(skills);
        response.setAgents(agents);
        return response;
    }

    private Set<String> recallAgentsByKeyword(String query, int topK) {
        String keyword = escapeQuery(query);
        Query redisQuery = new Query("@description:(" + keyword + ")")
                .limit(0, topK)
                .setNoStopwords();
        SearchResult result = jedis.ftSearch(redisProperties.getAgentIndex(), redisQuery);
        return result.getDocuments().stream()
                .map(Document::getId)
                .map(this::parseAgentId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    private Set<String> recallAgentsByVector(List<Float> embedding, int topK) {
        if (CollUtil.isEmpty(embedding)) {
            return Collections.emptySet();
        }
        byte[] vector = VectorUtils.toFloat32ByteArray(embedding);
        Query redisQuery = new Query("*=>[KNN " + topK + " @embedding $vec AS vector_score]")
                .addParam("vec", vector)
                .setSortBy("vector_score", true)
                .returnFields("agentId", "vector_score")
                .dialect(2);
        SearchResult result = jedis.ftSearch(redisProperties.getAgentIndex(), redisQuery);
        return result.getDocuments().stream()
                .map(Document::getId)
                .map(this::parseAgentId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    private List<RankedSkill> recallSkillsByFallback(String query, List<Float> embedding, Set<String> knownAgentIds) {
        Set<String> keywordSkills = recallSkillsByKeyword(query, discoveryProperties.getSkillRecallTopK());
        Set<String> vectorSkills = recallSkillsByVector(embedding, discoveryProperties.getSkillRecallTopK());
        Set<String> skillKeys = union(keywordSkills, vectorSkills);
        List<RankedSkill> results = new ArrayList<>();
        for (String key : skillKeys) {
            SkillDocument skill = jedis.jsonGet(key, SkillDocument.class, Path.ROOT_PATH);
            if (skill == null || StrUtil.isBlank(skill.getAgentId())) {
                continue;
            }
            if (!skill.isAgentActive()) {
                continue;
            }
            if (knownAgentIds.contains(skill.getAgentId())) {
                continue;
            }
            double baseScore = 0;
            if (keywordSkills.contains(key)) {
                baseScore += 1.0;
            }
            if (vectorSkills.contains(key)) {
                baseScore += 1.0;
            }
            RankedSkill ranked = new RankedSkill(toExpandedSkill(skill), baseScore);
            results.add(ranked);
        }
        return results;
    }

    private Set<String> recallSkillsByKeyword(String query, int topK) {
        String keyword = escapeQuery(query);
        String tagQuery = buildTagQuery(query);
        String redisQueryString = StrUtil.isBlank(tagQuery)
                ? "@skillDescription:(" + keyword + ")"
                : "(@skillDescription:(" + keyword + ")|@tags:{" + tagQuery + "})";
        Query redisQuery = new Query(redisQueryString)
                .limit(0, topK)
                .setNoStopwords();
        SearchResult result = jedis.ftSearch(redisProperties.getSkillIndex(), redisQuery);
        return result.getDocuments().stream()
                .map(Document::getId)
                .collect(Collectors.toSet());
    }

    private Set<String> recallSkillsByVector(List<Float> embedding, int topK) {
        if (CollUtil.isEmpty(embedding)) {
            return Collections.emptySet();
        }
        byte[] vector = VectorUtils.toFloat32ByteArray(embedding);
        Query redisQuery = new Query("*=>[KNN " + topK + " @embedding $vec AS vector_score]")
                .addParam("vec", vector)
                .setSortBy("vector_score", true)
                .returnFields("skillId", "vector_score")
                .dialect(2);
        SearchResult result = jedis.ftSearch(redisProperties.getSkillIndex(), redisQuery);
        return result.getDocuments().stream()
                .map(Document::getId)
                .collect(Collectors.toSet());
    }

    private List<AgentCardDocument> fetchAgents(Set<String> agentIds, Integer maxAgents) {
        if (CollUtil.isEmpty(agentIds)) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<>(agentIds);
        if (maxAgents != null && maxAgents > 0 && ids.size() > maxAgents) {
            ids = ids.subList(0, maxAgents);
        }
        String[] keys = ids.stream()
                .map(id -> AgentKeyBuilder.agentKey(redisProperties.getAgentKeyPrefix(), id))
                .toArray(String[]::new);
        List<AgentCardDocument> docs = jedis.jsonMGet(Path.ROOT_PATH, AgentCardDocument.class, keys);
        return docs.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<AgentCard> fetchAgentCards(Set<String> agentIds) {
        if (CollUtil.isEmpty(agentIds)) {
            return Collections.emptyList();
        }
        String[] keys = agentIds.stream()
                .map(id -> AgentKeyBuilder.agentKey(redisProperties.getAgentKeyPrefix(), id))
                .toArray(String[]::new);
        List<AgentCardDocument> docs = jedis.jsonMGet(Path.ROOT_PATH, AgentCardDocument.class, keys);
        List<AgentCard> agents = new ArrayList<>();
        for (AgentCardDocument doc : docs) {
            if (doc == null) {
                continue;
            }
            agents.add(BeanUtil.copyProperties(doc, AgentCard.class));
        }
        return agents;
    }

    private List<RankedSkill> expandSkillsFromAgents(List<AgentCardDocument> agents,
                                                     Set<String> keywordAgents,
                                                     Set<String> vectorAgents) {
        if (CollUtil.isEmpty(agents)) {
            return Collections.emptyList();
        }
        List<RankedSkill> results = new ArrayList<>();
        for (AgentCardDocument agent : agents) {
            if (agent == null || CollUtil.isEmpty(agent.getSkills())) {
                continue;
            }
            if (!agent.isActive()) {
                continue;
            }
            double baseScore = 0;
            if (keywordAgents.contains(agent.getAgentId())) {
                baseScore += 1.0;
            }
            if (vectorAgents.contains(agent.getAgentId())) {
                baseScore += 1.0;
            }
            for (SkillCard skill : agent.getSkills()) {
                ExpandedSkill expanded = toExpandedSkill(agent, skill);
                results.add(new RankedSkill(expanded, baseScore));
            }
        }
        return results;
    }

    private ExpandedSkill toExpandedSkill(AgentCard agent, SkillCard skill) {
        ExpandedSkill expanded = new ExpandedSkill();
        expanded.setAgentId(agent.getAgentId());
        expanded.setAgentName(agent.getName());
        expanded.setAgentDescription(agent.getDescription());
        expanded.setAgentActive(agent.isActive());
        expanded.setEndpoint(agent.getEndpoint());

        expanded.setSkillId(skill.getId());
        expanded.setSkillName(skill.getName());
        expanded.setSkillDescription(skill.getDescription());
        expanded.setTags(skill.getTags());
        expanded.setInputModes(skill.getInputModes());
        expanded.setOutputModes(skill.getOutputModes());
        expanded.setVersion(skill.getVersion());
        expanded.setExamples(skill.getExamples());
        return expanded;
    }

    private ExpandedSkill toExpandedSkill(SkillDocument skill) {
        ExpandedSkill expanded = new ExpandedSkill();
        expanded.setAgentId(skill.getAgentId());
        expanded.setAgentName(skill.getAgentName());
        expanded.setAgentDescription(skill.getAgentDescription());
        expanded.setAgentActive(skill.isAgentActive());
        expanded.setEndpoint(skill.getEndpoint());

        expanded.setSkillId(skill.getSkillId());
        expanded.setSkillName(skill.getSkillName());
        expanded.setSkillDescription(skill.getSkillDescription());
        expanded.setTags(skill.getTags());
        expanded.setInputModes(skill.getInputModes());
        expanded.setOutputModes(skill.getOutputModes());
        expanded.setVersion(skill.getVersion());
        expanded.setExamples(skill.getExamples());
        return expanded;
    }

    private List<RankedSkill> coarseRank(List<RankedSkill> candidates, String query, int topK) {
        if (CollUtil.isEmpty(candidates)) {
            return Collections.emptyList();
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        for (RankedSkill candidate : candidates) {
            double score = candidate.score;
            if (containsIgnoreCase(candidate.skill.getSkillName(), normalized)) {
                score += 0.6;
            }
            if (containsIgnoreCase(candidate.skill.getSkillDescription(), normalized)) {
                score += 1.0;
            }
            if (containsIgnoreCase(candidate.skill.getAgentDescription(), normalized)) {
                score += 0.3;
            }
            if (CollUtil.isNotEmpty(candidate.skill.getTags())) {
                for (String tag : candidate.skill.getTags()) {
                    if (containsIgnoreCase(tag, normalized) || normalized.contains(tag.toLowerCase(Locale.ROOT))) {
                        score += 0.2;
                    }
                }
            }
            candidate.score = score;
        }
        return candidates.stream()
                .sorted(Comparator.comparingDouble(RankedSkill::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    private List<RankedSkill> fineRank(List<RankedSkill> candidates, String query, int topK) {
        if (CollUtil.isEmpty(candidates)) {
            return Collections.emptyList();
        }
        List<RankedSkill> topCandidates = candidates.stream()
                .sorted(Comparator.comparingDouble(RankedSkill::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());

        String prompt = buildRankingPrompt(query, topCandidates);
        if (StrUtil.isBlank(prompt)) {
            return candidates;
        }
        String response = dashScopeClient.chat(List.of(
                new DashScopeMessage("system", "You are a ranking engine. Return strict JSON."),
                new DashScopeMessage("user", prompt)
        ));
        Map<String, Double> llmScores = parseLlmScores(response);
        if (llmScores.isEmpty()) {
            return candidates;
        }
        for (RankedSkill candidate : topCandidates) {
            Double score = llmScores.get(candidate.skill.getSkillId());
            if (score != null) {
                candidate.score = candidate.score + score / 100.0;
            }
        }
        return candidates.stream()
                .sorted(Comparator.comparingDouble(RankedSkill::getScore).reversed())
                .collect(Collectors.toList());
    }

    private List<RankedSkill> applyDiversity(List<RankedSkill> ranked, int topK, int maxSkillsPerAgent) {
        if (CollUtil.isEmpty(ranked)) {
            return Collections.emptyList();
        }
        Map<String, Integer> agentCount = new HashMap<>();
        List<RankedSkill> results = new ArrayList<>();
        for (RankedSkill candidate : ranked) {
            if (results.size() >= topK) {
                break;
            }
            String agentId = candidate.skill.getAgentId();
            int count = agentCount.getOrDefault(agentId, 0);
            if (count >= maxSkillsPerAgent) {
                continue;
            }
            agentCount.put(agentId, count + 1);
            results.add(candidate);
        }
        return results;
    }

    private LlmSkill toLlmSkill(RankedSkill ranked) {
        ExpandedSkill skill = ranked.skill;
        LlmSkill llmSkill = new LlmSkill();
        llmSkill.setAgentId(skill.getAgentId());
        llmSkill.setSkillId(skill.getSkillId());
        llmSkill.setName(skill.getSkillName());
        llmSkill.setDescription(skill.getSkillDescription());
        llmSkill.setTags(skill.getTags());
        llmSkill.setInputModes(skill.getInputModes());
        llmSkill.setOutputModes(skill.getOutputModes());
        llmSkill.setEndpoint(skill.getEndpoint());
        llmSkill.setAgentName(skill.getAgentName());
        llmSkill.setAgentDescription(skill.getAgentDescription());
        return llmSkill;
    }

    private String buildRankingPrompt(String query, List<RankedSkill> candidates) {
        if (CollUtil.isEmpty(candidates)) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Query: ").append(query).append("\n");
        builder.append("Return a JSON array of {skillId, score} where score is 0-100.\n");
        builder.append("Candidates:\n");
        int index = 1;
        for (RankedSkill candidate : candidates) {
            ExpandedSkill skill = candidate.skill;
            builder.append(index++).append(") ")
                    .append("skillId=").append(skill.getSkillId()).append("; ")
                    .append("name=").append(skill.getSkillName()).append("; ")
                    .append("description=").append(skill.getSkillDescription()).append("; ")
                    .append("tags=").append(skill.getTags()).append("; ")
                    .append("agentName=").append(skill.getAgentName()).append("; ")
                    .append("agentDescription=").append(skill.getAgentDescription()).append("\n");
        }
        return builder.toString();
    }

    private Map<String, Double> parseLlmScores(String response) {
        if (StrUtil.isBlank(response)) {
            return Collections.emptyMap();
        }
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return Collections.emptyMap();
        }
        String json = response.substring(start, end + 1);
        Map<String, Double> scores = new HashMap<>();
        try {
            JSONArray array = JSONUtil.parseArray(json);
            for (int i = 0; i < array.size(); i++) {
                String skillId = array.getJSONObject(i).getStr("skillId");
                Double score = array.getJSONObject(i).getDouble("score");
                if (StrUtil.isNotBlank(skillId) && score != null) {
                    scores.put(skillId, score);
                }
            }
        } catch (Exception e) {
            return Collections.emptyMap();
        }
        return scores;
    }

    private boolean containsIgnoreCase(String text, String query) {
        if (StrUtil.isBlank(text) || StrUtil.isBlank(query)) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(query);
    }

    private String escapeQuery(String query) {
        String sanitized = query.replace("\"", " ").trim();
        return StrUtil.blankToDefault(sanitized, query);
    }

    private String buildTagQuery(String query) {
        String[] tokens = query.split("\\s+");
        List<String> cleaned = new ArrayList<>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.length() > 1) {
                cleaned.add(trimmed);
            }
        }
        return String.join("|", cleaned);
    }

    private List<RankedSkill> dedupeCandidates(List<RankedSkill> candidates) {
        if (CollUtil.isEmpty(candidates)) {
            return Collections.emptyList();
        }
        Map<String, RankedSkill> unique = new LinkedHashMap<>();
        for (RankedSkill candidate : candidates) {
            String key = candidate.skill.getAgentId() + "::" + candidate.skill.getSkillId();
            RankedSkill existing = unique.get(key);
            if (existing == null || candidate.score > existing.score) {
                unique.put(key, candidate);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private Set<String> union(Set<String> first, Set<String> second) {
        Set<String> result = new HashSet<>();
        if (first != null) {
            result.addAll(first);
        }
        if (second != null) {
            result.addAll(second);
        }
        return result;
    }

    private String parseAgentId(String key) {
        String prefix = redisProperties.getAgentKeyPrefix();
        if (StrUtil.isNotBlank(prefix) && key.startsWith(prefix)) {
            return key.substring(prefix.length());
        }
        return key;
    }

    private static final class RankedSkill {
        private final ExpandedSkill skill;
        private double score;

        private RankedSkill(ExpandedSkill skill, double score) {
            this.skill = skill;
            this.score = score;
        }

        private double getScore() {
            return score;
        }
    }
}
