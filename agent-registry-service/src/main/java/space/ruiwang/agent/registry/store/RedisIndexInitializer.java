package space.ruiwang.agent.registry.store;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;
import space.ruiwang.agent.registry.config.RedisStackProperties;

@Slf4j
@Component
public class RedisIndexInitializer {
    private final JedisPooled jedis;
    private final RedisStackProperties properties;

    public RedisIndexInitializer(JedisPooled jedis, RedisStackProperties properties) {
        this.jedis = jedis;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        ensureIndex(properties.getAgentIndex(), buildAgentSchema(), properties.getAgentKeyPrefix());
        ensureIndex(properties.getSkillIndex(), buildSkillSchema(), properties.getSkillKeyPrefix());
    }

    private void ensureIndex(String indexName, Schema schema, String prefix) {
        try {
            jedis.ftInfo(indexName);
            log.info("Redis index [{}] exists", indexName);
        } catch (Exception e) {
            IndexDefinition definition = new IndexDefinition(IndexDefinition.Type.JSON)
                    .setPrefixes(prefix);
            IndexOptions options = IndexOptions.defaultOptions().setDefinition(definition).setNoStopwords();
            jedis.ftCreate(indexName, options, schema);
            log.info("Redis index [{}] created", indexName);
        }
    }

    private Schema buildAgentSchema() {
        Map<String, Object> vectorParams = new HashMap<>();
        vectorParams.put("TYPE", "FLOAT32");
        vectorParams.put("DIM", properties.getVectorDimension());
        vectorParams.put("DISTANCE_METRIC", properties.getDistanceMetric());
        vectorParams.put("M", properties.getHnswM());
        vectorParams.put("EF_CONSTRUCTION", properties.getHnswEfConstruction());
        return new Schema()
                .addTagField("$.agentId").as("agentId")
                .addTextField("$.name", 1.0).as("name")
                .addTextField("$.description", 1.0).as("description")
                .addVectorField("$.embedding", Schema.VectorField.VectorAlgo.HNSW, vectorParams).as("embedding");
    }

    private Schema buildSkillSchema() {
        Map<String, Object> vectorParams = new HashMap<>();
        vectorParams.put("TYPE", "FLOAT32");
        vectorParams.put("DIM", properties.getVectorDimension());
        vectorParams.put("DISTANCE_METRIC", properties.getDistanceMetric());
        vectorParams.put("M", properties.getHnswM());
        vectorParams.put("EF_CONSTRUCTION", properties.getHnswEfConstruction());
        return new Schema()
                .addTagField("$.skillId").as("skillId")
                .addTagField("$.agentId").as("agentId")
                .addTextField("$.skillName", 1.0).as("skillName")
                .addTextField("$.skillDescription", 1.0).as("skillDescription")
                .addTagField("$.tags").as("tags")
                .addVectorField("$.embedding", Schema.VectorField.VectorAlgo.HNSW, vectorParams).as("embedding");
    }
}
