package com.familywishes.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tiny builder for Gemini function-declaration parameter schemas. */
public final class ToolSchema {
  private final Map<String, Object> properties = new LinkedHashMap<>();
  private final List<String> required = new ArrayList<>();

  private ToolSchema() {}

  public static ToolSchema create() {
    return new ToolSchema();
  }

  public ToolSchema string(String name, String description, boolean isRequired) {
    return add(name, "string", description, isRequired);
  }

  public ToolSchema integer(String name, String description, boolean isRequired) {
    return add(name, "integer", description, isRequired);
  }

  private ToolSchema add(String name, String type, String description, boolean isRequired) {
    properties.put(name, Map.of("type", type, "description", description));
    if (isRequired) {
      required.add(name);
    }
    return this;
  }

  /** Returns null when there are no parameters (Gemini rejects an empty properties object). */
  public Map<String, Object> build() {
    if (properties.isEmpty()) {
      return null;
    }
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("properties", properties);
    if (!required.isEmpty()) {
      schema.put("required", required);
    }
    return schema;
  }
}
