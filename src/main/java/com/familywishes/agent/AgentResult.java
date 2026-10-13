package com.familywishes.agent;

import java.util.List;
import java.util.Map;

/**
 * @param completed true if the run ended normally (final text or accepted terminal tool)
 * @param finalText the model's last plain-text answer, if any
 * @param terminalArgs arguments of the accepted terminal tool call, if any
 * @param trace human-readable log of tool calls, useful for audit
 */
public record AgentResult(
    boolean completed,
    String finalText,
    Map<String, Object> terminalArgs,
    List<String> trace,
    int steps) {}
