package com.familywishes.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Minimal tool-calling loop: model -> tool calls -> tool results -> model ... until the model
 * answers in text, a terminal tool is accepted, or maxSteps is hit.
 *
 * <p>Guardrails: hard step cap, unknown tools and tool exceptions are fed back to the model as
 * errors (never thrown), and the model can only reach tools the caller put in the spec.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentRunner {
  private final GeminiAgentClient client;
  private final ObjectMapper objectMapper;

  @SuppressWarnings("unchecked")
  public AgentResult run(AgentSpec spec) {
    Map<String, AgentTool> toolsByName = new HashMap<>();
    spec.tools().forEach(t -> toolsByName.put(t.name(), t));

    List<Map<String, Object>> contents = new ArrayList<>();
    contents.add(content("user", List.of(Map.of("text", spec.userPrompt()))));
    List<String> trace = new ArrayList<>();

    for (int step = 1; step <= spec.maxSteps(); step++) {
      Map<String, Object> response = client.generate(buildBody(spec, contents));
      Map<String, Object> modelContent = firstCandidateContent(response);
      if (modelContent == null || modelContent.get("parts") == null) {
        trace.add("step " + step + ": empty model response");
        return new AgentResult(false, null, null, trace, step);
      }

      // Keep the model turn exactly as returned (preserves thought signatures on newer models).
      contents.add(modelContent);
      List<Map<String, Object>> parts = (List<Map<String, Object>>) modelContent.get("parts");
      List<Map<String, Object>> calls =
          parts.stream().filter(p -> p.containsKey("functionCall")).toList();

      if (calls.isEmpty()) {
        String text =
            parts.stream()
                .map(p -> (String) p.get("text"))
                .filter(t -> t != null)
                .collect(Collectors.joining("\n"))
                .trim();
        return new AgentResult(true, text, null, trace, step);
      }

      List<Map<String, Object>> responseParts = new ArrayList<>();
      Map<String, Object> acceptedTerminalArgs = null;

      for (Map<String, Object> part : calls) {
        Map<String, Object> call = (Map<String, Object>) part.get("functionCall");
        String name = String.valueOf(call.get("name"));
        Map<String, Object> args =
            call.get("args") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();

        Object result;
        AgentTool tool = toolsByName.get(name);
        if (tool == null) {
          result = Map.of("error", "Unknown tool: " + name);
        } else {
          try {
            result = tool.execute(args);
          } catch (Exception e) {
            log.warn("Agent tool {} failed: {}", name, e.getMessage());
            result = Map.of("error", String.valueOf(e.getMessage()));
          }
        }
        trace.add("step " + step + ": " + name + " " + summarize(args));

        Map<String, Object> asMap = toResponseMap(result);
        responseParts.add(Map.of("functionResponse", Map.of("name", name, "response", asMap)));

        if (spec.terminalTools().contains(name) && Boolean.TRUE.equals(asMap.get("accepted"))) {
          acceptedTerminalArgs = args;
        }
      }

      if (acceptedTerminalArgs != null) {
        return new AgentResult(true, null, acceptedTerminalArgs, trace, step);
      }
      contents.add(content("user", responseParts));
    }

    trace.add("max steps reached");
    return new AgentResult(false, null, null, trace, spec.maxSteps());
  }

  private Map<String, Object> buildBody(AgentSpec spec, List<Map<String, Object>> contents) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", spec.systemPrompt()))));
    body.put("contents", contents);
    if (!spec.tools().isEmpty()) {
      List<Map<String, Object>> declarations = new ArrayList<>();
      for (AgentTool t : spec.tools()) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("name", t.name());
        d.put("description", t.description());
        if (t.parameters() != null) {
          d.put("parameters", t.parameters());
        }
        declarations.add(d);
      }
      body.put("tools", List.of(Map.of("functionDeclarations", declarations)));
      body.put(
          "toolConfig",
          Map.of(
              "functionCallingConfig", Map.of("mode", spec.forceToolCall() ? "ANY" : "AUTO")));
    }
    return body;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> firstCandidateContent(Map<String, Object> response) {
    if (response == null) return null;
    var candidates = (List<Map<String, Object>>) response.get("candidates");
    if (candidates == null || candidates.isEmpty()) return null;
    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
    if (content == null) return null;
    Map<String, Object> copy = new LinkedHashMap<>(content);
    copy.putIfAbsent("role", "model");
    return copy;
  }

  private static Map<String, Object> content(String role, List<Map<String, Object>> parts) {
    Map<String, Object> c = new LinkedHashMap<>();
    c.put("role", role);
    c.put("parts", parts);
    return c;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> toResponseMap(Object result) {
    if (result == null) return Map.of("result", "ok");
    if (result instanceof Map<?, ?> m) return (Map<String, Object>) m;
    if (result instanceof List<?> || result instanceof String || result instanceof Number) {
      return Map.of("result", result);
    }
    return objectMapper.convertValue(result, Map.class);
  }

  private static String summarize(Map<String, Object> args) {
    String s = String.valueOf(args);
    return s.length() > 200 ? s.substring(0, 200) + "..." : s;
  }
}
