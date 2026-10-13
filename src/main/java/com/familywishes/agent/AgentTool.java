package com.familywishes.agent;

import java.util.Map;
import java.util.function.Function;

/** A capability the model may invoke. Implementations must be side-effect safe or guard themselves. */
public interface AgentTool {
  String name();

  String description();

  /** JSON-schema "object" describing the arguments (see {@link ToolSchema}). */
  Map<String, Object> parameters();

  Object execute(Map<String, Object> args);

  static AgentTool of(
      String name,
      String description,
      Map<String, Object> parameters,
      Function<Map<String, Object>, Object> handler) {
    return new AgentTool() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public String description() {
        return description;
      }

      @Override
      public Map<String, Object> parameters() {
        return parameters;
      }

      @Override
      public Object execute(Map<String, Object> args) {
        return handler.apply(args);
      }
    };
  }
}
