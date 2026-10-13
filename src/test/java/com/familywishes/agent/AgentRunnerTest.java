package com.familywishes.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AgentRunnerTest {

  private static Map<String, Object> modelCall(String tool, Map<String, Object> args) {
    return Map.of(
        "candidates",
        List.of(
            Map.of(
                "content",
                Map.of(
                    "role",
                    "model",
                    "parts",
                    List.of(Map.of("functionCall", Map.of("name", tool, "args", args)))))));
  }

  private static Map<String, Object> modelText(String text) {
    return Map.of(
        "candidates",
        List.of(Map.of("content", Map.of("role", "model", "parts", List.of(Map.of("text", text))))));
  }

  @Test
  void stopsWhenTerminalToolAccepted() {
    GeminiAgentClient client = mock(GeminiAgentClient.class);
    when(client.generate(anyMap()))
        .thenReturn(modelCall("submit", Map.of("subject", "Hi", "htmlMessage", "<p>Hello</p>")));
    AgentTool submit =
        AgentTool.of("submit", "submit", null, a -> Map.of("accepted", true));

    AgentResult r =
        new AgentRunner(client, new ObjectMapper())
            .run(new AgentSpec("sys", "go", List.of(submit), 5, Set.of("submit"), true));

    assertTrue(r.completed());
    assertEquals("Hi", r.terminalArgs().get("subject"));
    assertEquals(1, r.steps());
  }

  @Test
  void feedsRejectionBackAndRetries() {
    GeminiAgentClient client = mock(GeminiAgentClient.class);
    when(client.generate(anyMap()))
        .thenReturn(modelCall("submit", Map.of("subject", "bad")))
        .thenReturn(modelCall("submit", Map.of("subject", "good")));
    AgentTool submit =
        AgentTool.of(
            "submit",
            "submit",
            null,
            a -> "good".equals(a.get("subject")) ? Map.of("accepted", true) : Map.of("accepted", false));

    AgentResult r =
        new AgentRunner(client, new ObjectMapper())
            .run(new AgentSpec("sys", "go", List.of(submit), 5, Set.of("submit"), true));

    assertTrue(r.completed());
    assertEquals(2, r.steps());
  }

  @Test
  void unknownToolAndMaxStepsDoNotThrow() {
    GeminiAgentClient client = mock(GeminiAgentClient.class);
    when(client.generate(anyMap())).thenReturn(modelCall("nope", Map.of()));

    AgentResult r =
        new AgentRunner(client, new ObjectMapper())
            .run(new AgentSpec("sys", "go", List.of(), 3, Set.of(), false));

    assertFalse(r.completed());
    assertEquals(3, r.steps());
  }

  @Test
  void returnsFinalTextWhenModelAnswersDirectly() {
    GeminiAgentClient client = mock(GeminiAgentClient.class);
    when(client.generate(anyMap())).thenReturn(modelText("All good."));

    AgentResult r =
        new AgentRunner(client, new ObjectMapper())
            .run(new AgentSpec("sys", "go", List.of(), 3, Set.of(), false));

    assertTrue(r.completed());
    assertEquals("All good.", r.finalText());
  }
}
