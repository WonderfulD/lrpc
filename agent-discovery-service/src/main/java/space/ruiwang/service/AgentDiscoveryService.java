package space.ruiwang.service;

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
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;
import space.ruiwang.agent.dashscope.DashScopeClient;
import space.ruiwang.agent.dashscope.DashScopeMessage;
import space.ruiwang.config.DiscoveryProperties;
import space.ruiwang.config.RedisStackProperties;
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

@Slf4j
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

        Set<String> excludedAgentIds = buildExcludedAgentIds(request);
        log.debug("Discovery start query='{}' topK={} maxAgents={} excludedAgents={}",
                query, topK, request.getMaxAgents(), excludedAgentIds.size());
        List<Float> queryEmbedding = dashScopeClient.embed(query);
        log.debug("Query embedding size={}", queryEmbedding == null ? 0 : queryEmbedding.size());
        Set<String> keywordAgents = recallAgentsByKeyword(query, discoveryProperties.getAgentRecallTopK());
        Map<String, Double> vectorAgents = recallAgentsByVector(queryEmbedding, discoveryProperties.getAgentRecallTopK());
        log.debug("Agent recall keyword={} vector={}", keywordAgents.size(), vectorAgents.size());
        if (CollUtil.isNotEmpty(excludedAgentIds)) {
            int keywordBefore = keywordAgents.size();
            int vectorBefore = vectorAgents.size();
            filterAgentRecallByExcluded(keywordAgents, vectorAgents, excludedAgentIds);
            log.debug("Agent recall after exclude keyword={} (removed {}) vector={} (removed {})",
                    keywordAgents.size(), keywordBefore - keywordAgents.size(),
                    vectorAgents.size(), vectorBefore - vectorAgents.size());
        }
        if (log.isTraceEnabled() && !vectorAgents.isEmpty()) {
            log.trace("Agent vector scores sample: {}", summarizeVectorScores(vectorAgents, 8));
        }
        Set<String> agentIds = union(keywordAgents, vectorAgents.keySet());
        log.debug("Agent recall union={}", agentIds.size());
        agentIds = filterAgentsByThreshold(agentIds,
                keywordAgents,
                vectorAgents,
                discoveryProperties.getAgentVectorDistanceThreshold());
        log.debug("Agents after threshold={} (similarityThreshold={})", agentIds.size(),
                discoveryProperties.getAgentVectorDistanceThreshold());
        agentIds.removeAll(excludedAgentIds);
        log.debug("Agents after exclude={}", agentIds.size());

        List<AgentCardDocument> agentDocs = fetchAgents(agentIds, request.getMaxAgents());
        List<RankedSkill> candidates = expandSkillsFromAgents(agentDocs, keywordAgents, vectorAgents, excludedAgentIds);
        log.debug("Expanded skills from agents={}", candidates.size());

        if (agentIds.size() < discoveryProperties.getMinAgentRecall()) {
            log.debug("Agent recall {} below min {}, falling back to skill recall",
                    agentIds.size(), discoveryProperties.getMinAgentRecall());
            List<RankedSkill> fallback = recallSkillsByFallback(query, queryEmbedding, agentIds, excludedAgentIds);
            log.debug("Fallback skills={}", fallback.size());
            candidates.addAll(fallback);
        }

        candidates = dedupeCandidates(candidates);
        log.debug("Candidates after dedupe={}", candidates.size());
        List<RankedSkill> coarseRanked = coarseRank(candidates, query, discoveryProperties.getCoarseTopK());
        log.debug("Candidates after coarse rank={}", coarseRanked.size());
        List<RankedSkill> fineRanked = fineRank(coarseRanked, query, discoveryProperties.getFineTopK());
        log.debug("Candidates after fine rank={}", fineRanked.size());
        Double minScore = request.getMinScore();
        if (minScore != null && minScore > 0) {
            fineRanked = filterByMinScore(fineRanked, minScore);
            log.debug("Candidates after minScore filter={} (minScore={})", fineRanked.size(), minScore);
        }

        List<RankedSkill> selected = applyDiversity(fineRanked, topK, discoveryProperties.getMaxSkillsPerAgent());
        log.debug("Selected skills={}", selected.size());
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

    private Map<String, Double> recallAgentsByVector(List<Float> embedding, int topK) {
        if (CollUtil.isEmpty(embedding)) {
            return Collections.emptyMap();
        }
        byte[] vector = VectorUtils.toFloat32ByteArray(embedding);
        Query redisQuery = new Query("*=>[KNN " + topK + " @embedding $vec AS vector_score]")
                .addParam("vec", vector)
                .setSortBy("vector_score", true)
                .returnFields("agentId", "vector_score")
                .dialect(2);
        SearchResult result = jedis.ftSearch(redisProperties.getAgentIndex(), redisQuery);
        Map<String, Double> scores = new HashMap<>();
        for (Document doc : result.getDocuments()) {
            String agentId = parseAgentId(doc.getId());
            if (StrUtil.isBlank(agentId)) {
                continue;
            }
            Double score = parseVectorScore(doc);
            scores.put(agentId, score);
        }
        return scores;
    }

    private List<RankedSkill> recallSkillsByFallback(String query,
                                                     List<Float> embedding,
                                                     Set<String> knownAgentIds,
                                                     Set<String> excludedAgentIds) {
        Set<String> keywordSkills = recallSkillsByKeyword(query, discoveryProperties.getSkillRecallTopK());
        Map<String, Double> vectorSkills = recallSkillsByVector(embedding, discoveryProperties.getSkillRecallTopK());
        log.debug("Fallback recall keywordSkills={} vectorSkills={}", keywordSkills.size(), vectorSkills.size());
        if (CollUtil.isNotEmpty(excludedAgentIds) || CollUtil.isNotEmpty(knownAgentIds)) {
            int keywordBefore = keywordSkills.size();
            int vectorBefore = vectorSkills.size();
            filterSkillRecallByAgent(keywordSkills, vectorSkills, excludedAgentIds, knownAgentIds);
            log.debug("Fallback recall after exclude keywordSkills={} (removed {}) vectorSkills={} (removed {})",
                    keywordSkills.size(), keywordBefore - keywordSkills.size(),
                    vectorSkills.size(), vectorBefore - vectorSkills.size());
        }
        if (log.isTraceEnabled() && !vectorSkills.isEmpty()) {
            log.trace("Skill vector scores sample: {}", summarizeVectorScores(vectorSkills, 8));
        }
        Set<String> skillKeys = union(keywordSkills, vectorSkills.keySet());
        log.debug("Fallback skills union={}", skillKeys.size());
        List<RankedSkill> results = new ArrayList<>();
        int thresholdRejected = 0;
        int inactiveRejected = 0;
        int knownAgentRejected = 0;
        int excludedAgentRejected = 0;
        int missingRejected = 0;
        for (String key : skillKeys) {
            if (!passesVectorThreshold(key, keywordSkills, vectorSkills, discoveryProperties.getSkillVectorDistanceThreshold())) {
                thresholdRejected++;
                continue;
            }
            SkillDocument skill = jedis.jsonGet(key, SkillDocument.class, Path.ROOT_PATH);
            if (skill == null || StrUtil.isBlank(skill.getAgentId())) {
                missingRejected++;
                continue;
            }
            if (!skill.isAgentActive()) {
                inactiveRejected++;
                continue;
            }
            if (knownAgentIds.contains(skill.getAgentId())) {
                knownAgentRejected++;
                continue;
            }
            if (excludedAgentIds.contains(skill.getAgentId())) {
                excludedAgentRejected++;
                continue;
            }
            double baseScore = 0;
            if (keywordSkills.contains(key)) {
                baseScore += 1.0;
            }
            if (vectorSkills.containsKey(key)) {
                baseScore += 1.0;
            }
            RankedSkill ranked = new RankedSkill(toExpandedSkill(skill), baseScore);
            results.add(ranked);
        }
        log.debug("Fallback filters: thresholdRejected={} inactiveRejected={} knownAgentRejected={} excludedAgentRejected={} missingRejected={}",
                thresholdRejected, inactiveRejected, knownAgentRejected, excludedAgentRejected, missingRejected);
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

    private Map<String, Double> recallSkillsByVector(List<Float> embedding, int topK) {
        if (CollUtil.isEmpty(embedding)) {
            return Collections.emptyMap();
        }
        byte[] vector = VectorUtils.toFloat32ByteArray(embedding);
        Query redisQuery = new Query("*=>[KNN " + topK + " @embedding $vec AS vector_score]")
                .addParam("vec", vector)
                .setSortBy("vector_score", true)
                .returnFields("skillId", "vector_score")
                .dialect(2);
        SearchResult result = jedis.ftSearch(redisProperties.getSkillIndex(), redisQuery);
        Map<String, Double> scores = new HashMap<>();
        for (Document doc : result.getDocuments()) {
            String key = doc.getId();
            if (StrUtil.isBlank(key)) {
                continue;
            }
            Double score = parseVectorScore(doc);
            scores.put(key, score);
        }
        return scores;
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
                                                     Map<String, Double> vectorAgents,
                                                     Set<String> excludedAgentIds) {
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
            if (excludedAgentIds.contains(agent.getAgentId())) {
                continue;
            }
            double baseScore = 0;
            if (keywordAgents.contains(agent.getAgentId())) {
                baseScore += 1.0;
            }
            if (vectorAgents.containsKey(agent.getAgentId())) {
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
        if (agent.getProvider() != null) {
            expanded.setAgentOrganization(agent.getProvider().getOrganization());
        }
        expanded.setAgentVersion(agent.getVersion());

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
        expanded.setAgentOrganization(skill.getAgentOrganization());
        expanded.setAgentVersion(skill.getAgentVersion());

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
        llmSkill.setScore(ranked.score);
        llmSkill.setTags(skill.getTags());
        llmSkill.setInputModes(skill.getInputModes());
        llmSkill.setOutputModes(skill.getOutputModes());
        llmSkill.setEndpoint(skill.getEndpoint());
        llmSkill.setAgentName(skill.getAgentName());
        llmSkill.setAgentDescription(skill.getAgentDescription());
        llmSkill.setAgentOrganization(skill.getAgentOrganization());
        llmSkill.setAgentVersion(skill.getAgentVersion());
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

    private Set<String> buildExcludedAgentIds(AgentDiscoverRequest request) {
        Set<String> excluded = new HashSet<>();
        if (request == null) {
            return excluded;
        }
        if (StrUtil.isNotBlank(request.getCallerAgentId())) {
            excluded.add(request.getCallerAgentId().trim());
        }
        if (CollUtil.isNotEmpty(request.getExcludeAgentIds())) {
            for (String agentId : request.getExcludeAgentIds()) {
                if (StrUtil.isNotBlank(agentId)) {
                    excluded.add(agentId.trim());
                }
            }
        }
        return excluded;
    }

    private Set<String> filterAgentsByThreshold(Set<String> agentIds,
                                                Set<String> keywordAgents,
                                                Map<String, Double> vectorAgents,
                                                double threshold) {
        if (CollUtil.isEmpty(agentIds)) {
            return Collections.emptySet();
        }
        Set<String> filtered = new HashSet<>();
        int thresholdRejected = 0;
        for (String agentId : agentIds) {
            if (passesVectorThreshold(agentId, keywordAgents, vectorAgents, threshold)) {
                filtered.add(agentId);
            } else {
                thresholdRejected++;
            }
        }
        log.debug("Agent threshold rejected={}", thresholdRejected);
        return filtered;
    }

    private boolean passesVectorThreshold(String key,
                                          Set<String> keywordMatches,
                                          Map<String, Double> vectorScores,
                                          double threshold) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        Double score = vectorScores == null ? null : vectorScores.get(key);
        if (score != null) {
            double similarity = toSimilarityNormalized(score);
            return similarity >= threshold;
        }
        return keywordMatches != null && keywordMatches.contains(key);
    }

    private Double parseVectorScore(Document doc) {
        if (doc == null) {
            return null;
        }
        Object raw = doc.get("vector_score");
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return ((Number) raw).doubleValue();
        }
        String value = raw.toString();
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String summarizeVectorScores(Map<String, Double> scores, int limit) {
        return scores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(limit)
                .map(entry -> formatScoreSample(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    private List<RankedSkill> filterByMinScore(List<RankedSkill> candidates, double minScore) {
        if (CollUtil.isEmpty(candidates)) {
            return Collections.emptyList();
        }
        return candidates.stream()
                .filter(candidate -> candidate.score >= minScore)
                .collect(Collectors.toList());
    }

    private String formatScoreSample(String key, Double distance) {
        if (distance == null) {
            return key + "=null";
        }
        double similarity = toSimilarityNormalized(distance);
        return key + "=" + distance + "(sim=" + String.format(Locale.ROOT, "%.3f", similarity) + ")";
    }

    private double toSimilarityNormalized(double distance) {
        double similarity = 1.0 - (distance / 2.0);
        if (similarity < 0.0) {
            return 0.0;
        }
        if (similarity > 1.0) {
            return 1.0;
        }
        return similarity;
    }

    private String parseAgentId(String key) {
        String prefix = redisProperties.getAgentKeyPrefix();
        if (StrUtil.isNotBlank(prefix) && key.startsWith(prefix)) {
            return key.substring(prefix.length());
        }
        return key;
    }

    private String parseAgentIdFromSkillKey(String key) {
        if (StrUtil.isBlank(key)) {
            return "";
        }
        String prefix = redisProperties.getSkillKeyPrefix();
        String normalized = key;
        if (StrUtil.isNotBlank(prefix) && normalized.startsWith(prefix)) {
            normalized = normalized.substring(prefix.length());
        }
        int index = normalized.indexOf(':');
        if (index <= 0) {
            return "";
        }
        return normalized.substring(0, index);
    }

    private void filterAgentRecallByExcluded(Set<String> keywordAgents,
                                             Map<String, Double> vectorAgents,
                                             Set<String> excludedAgentIds) {
        if (CollUtil.isEmpty(excludedAgentIds)) {
            return;
        }
        if (CollUtil.isNotEmpty(keywordAgents)) {
            keywordAgents.removeAll(excludedAgentIds);
        }
        if (vectorAgents != null && !vectorAgents.isEmpty()) {
            vectorAgents.keySet().removeAll(excludedAgentIds);
        }
    }

    private void filterSkillRecallByAgent(Set<String> keywordSkills,
                                          Map<String, Double> vectorSkills,
                                          Set<String> excludedAgentIds,
                                          Set<String> knownAgentIds) {
        Set<String> blocked = new HashSet<>();
        if (CollUtil.isNotEmpty(excludedAgentIds)) {
            blocked.addAll(excludedAgentIds);
        }
        if (CollUtil.isNotEmpty(knownAgentIds)) {
            blocked.addAll(knownAgentIds);
        }
        if (blocked.isEmpty()) {
            return;
        }
        if (CollUtil.isNotEmpty(keywordSkills)) {
            for (var iterator = keywordSkills.iterator(); iterator.hasNext(); ) {
                String key = iterator.next();
                String agentId = parseAgentIdFromSkillKey(key);
                if (blocked.contains(agentId)) {
                    iterator.remove();
                }
            }
        }
        if (vectorSkills != null && !vectorSkills.isEmpty()) {
            for (var iterator = vectorSkills.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<String, Double> entry = iterator.next();
                String agentId = parseAgentIdFromSkillKey(entry.getKey());
                if (blocked.contains(agentId)) {
                    iterator.remove();
                }
            }
        }
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
