package com.familywishes.util;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TemplateRenderer {
    public String render(String template, Map<String, String> vars) {
        String out = template;
        for (var entry : vars.entrySet()) {
            out = out.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return out;
    }
}
