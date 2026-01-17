package space.ruiwang.agent.dashscope;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * Minimal DashScope client for embedding and chat.
 */
public class DashScopeClient {
    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com";
    private static final String DEFAULT_EMBEDDING_PATH = "/api/v1/services/embeddings/text-embedding/text-embedding";
    private static final String DEFAULT_CHAT_PATH = "/api/v1/services/aigc/text-generation/generation";

    private final DashScopeConfig config;
    private final HttpClient httpClient;

    public DashScopeClient(DashScopeConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
    }

    public List<Float> embed(String text) {
        if (StrUtil.isBlank(text)) {
            return Collections.emptyList();
        }
        String url = getBaseUrl() + getEmbeddingPath();
        JSONObject payload = JSONUtil.createObj()
                .set("model", config.getEmbeddingModel())
                .set("input", JSONUtil.createObj().set("texts", Collections.singletonList(text)));
        String response = postJson(url, payload.toString());
        JSONObject json = JSONUtil.parseObj(response);
        JSONObject output = json.getJSONObject("output");
        if (output == null) {
            return Collections.emptyList();
        }
        JSONArray embeddings = output.getJSONArray("embeddings");
        if (CollUtil.isEmpty(embeddings)) {
            return Collections.emptyList();
        }
        JSONObject first = embeddings.getJSONObject(0);
        JSONArray vector = first.getJSONArray("embedding");
        if (vector == null) {
            return Collections.emptyList();
        }
        List<Float> result = new ArrayList<>(vector.size());
        for (int i = 0; i < vector.size(); i++) {
            result.add(vector.getFloat(i));
        }
        return result;
    }

    public String chat(List<DashScopeMessage> messages) {
        if (CollUtil.isEmpty(messages)) {
            return "";
        }
        String url = getBaseUrl() + getChatPath();
        JSONArray messageArray = new JSONArray();
        for (DashScopeMessage message : messages) {
            messageArray.add(JSONUtil.createObj()
                    .set("role", message.getRole())
                    .set("content", message.getContent()));
        }
        JSONObject payload = JSONUtil.createObj()
                .set("model", config.getChatModel())
                .set("input", JSONUtil.createObj().set("messages", messageArray))
                .set("parameters", JSONUtil.createObj().set("result_format", "message"));
        String response = postJson(url, payload.toString());
        JSONObject json = JSONUtil.parseObj(response);
        JSONObject output = json.getJSONObject("output");
        if (output == null) {
            return "";
        }
        JSONArray choices = output.getJSONArray("choices");
        if (!CollUtil.isEmpty(choices)) {
            JSONObject first = choices.getJSONObject(0);
            JSONObject message = first.getJSONObject("message");
            if (message != null) {
                return message.getStr("content", "");
            }
        }
        return output.getStr("text", "");
    }

    private String postJson(String url, String body) {
        if (StrUtil.isBlank(config.getApiKey())) {
            throw new IllegalStateException("DashScope apiKey is not configured");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("DashScope request failed, status=" + response.statusCode()
                        + ", body=" + response.body());
            }
            return response.body();
        } catch (Exception e) {
            throw new IllegalStateException("DashScope request failed: " + e.getMessage(), e);
        }
    }

    private String getBaseUrl() {
        return StrUtil.blankToDefault(config.getBaseUrl(), DEFAULT_BASE_URL);
    }

    private String getEmbeddingPath() {
        return StrUtil.blankToDefault(config.getEmbeddingPath(), DEFAULT_EMBEDDING_PATH);
    }

    private String getChatPath() {
        return StrUtil.blankToDefault(config.getChatPath(), DEFAULT_CHAT_PATH);
    }
}
