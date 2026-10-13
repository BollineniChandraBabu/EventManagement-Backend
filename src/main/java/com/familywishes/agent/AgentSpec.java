package com.familywishes.agent;

import java.util.List;
import java.util.Set;

/**
 * @param terminalTools tools that end the run once they return {"accepted": true}
 * @param forceToolCall when true the model must call a tool every step (Gemini mode ANY)
 */
public record AgentSpec(
    String systemPrompt,
    String userPrompt,
    List<AgentTool> tools,
    int maxSteps,
    Set<String> terminalTools,
    boolean forceToolCall) {}
