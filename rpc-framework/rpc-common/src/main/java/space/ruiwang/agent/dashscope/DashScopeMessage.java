package space.ruiwang.agent.dashscope;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DashScope chat message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashScopeMessage implements Serializable {
    private String role;
    private String content;
}
