package com.familywishes.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Guards ehcache.xml: Spring fails at the first use of a cache that is not declared, and an
 * unbounded cache would slowly eat the memory of a small server.
 */
class CacheConfigurationTest {

  /** Hibernate needs this region to never expire, so it is the only cache without an expiry. */
  private static final String UPDATE_TIMESTAMPS_REGION = "default-update-timestamps-region";

  private static Document parseEhcacheXml() throws Exception {
    try (InputStream xml = CacheConfigurationTest.class.getResourceAsStream("/ehcache.xml")) {
      assertNotNull(xml, "ehcache.xml must be on the classpath");
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      return factory.newDocumentBuilder().parse(xml);
    }
  }

  private static Map<String, Element> declaredCaches() throws Exception {
    Map<String, Element> caches = new HashMap<>();
    NodeList nodes = parseEhcacheXml().getElementsByTagName("cache");
    for (int i = 0; i < nodes.getLength(); i++) {
      Element cache = (Element) nodes.item(i);
      caches.put(cache.getAttribute("alias"), cache);
    }
    return caches;
  }

  private static List<String> cacheNameConstants() throws IllegalAccessException {
    List<String> names = new ArrayList<>();
    for (Field field : CacheNames.class.getDeclaredFields()) {
      if (Modifier.isPublic(field.getModifiers()) && field.getType() == String.class) {
        names.add((String) field.get(null));
      }
    }
    return names;
  }

  @Test
  void everyCacheNameUsedInCodeIsDeclared() throws Exception {
    Map<String, Element> caches = declaredCaches();
    List<String> names = cacheNameConstants();

    assertFalse(names.isEmpty());
    for (String name : names) {
      assertTrue(caches.containsKey(name), "ehcache.xml has no <cache alias=\"" + name + "\">");
    }
  }

  @Test
  void everyCacheIsBoundedAndExpiresExceptTheUpdateTimestampsRegion() throws Exception {
    for (Map.Entry<String, Element> entry : declaredCaches().entrySet()) {
      String alias = entry.getKey();
      Element cache = entry.getValue();
      if (cache.hasAttribute("uses-template")) {
        continue; // size and expiry come from the template, which is checked below
      }
      assertEquals(1, cache.getElementsByTagName("heap").getLength(), alias + " needs a heap size");
      if (!UPDATE_TIMESTAMPS_REGION.equals(alias)) {
        assertEquals(1, cache.getElementsByTagName("ttl").getLength(), alias + " needs an expiry");
      }
    }
  }

  @Test
  void updateTimestampsRegionNeverExpires() throws Exception {
    Element region = declaredCaches().get(UPDATE_TIMESTAMPS_REGION);

    assertNotNull(region);
    assertEquals(0, region.getElementsByTagName("expiry").getLength());
  }

  @Test
  void cachesCreatedWithoutADeclarationUseABoundedTemplate() throws Exception {
    Document document = parseEhcacheXml();
    Element defaults = (Element) document.getElementsByTagNameNS("*", "defaults").item(0);
    String templateName = defaults.getAttribute("default-template");

    NodeList templates = document.getElementsByTagName("cache-template");
    Element template = null;
    for (int i = 0; i < templates.getLength(); i++) {
      if (templateName.equals(((Element) templates.item(i)).getAttribute("name"))) {
        template = (Element) templates.item(i);
      }
    }
    assertNotNull(template, "default-template '" + templateName + "' is not defined");
    assertEquals(1, template.getElementsByTagName("heap").getLength());
    assertEquals(1, template.getElementsByTagName("ttl").getLength());
  }
}
