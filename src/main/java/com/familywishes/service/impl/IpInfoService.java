package com.familywishes.service.impl;

import com.familywishes.config.CacheNames;
import com.familywishes.dto.DashboardDtos.ViolatedUserIpInfo;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Looks up where an IP address is located using ip-api.com.
 *
 * <p>Successful answers are cached for a day (see ehcache.xml): the free service allows only 45
 * requests per minute and an address rarely moves. Failures are not cached, so a rate-limit
 * response does not stick.
 */
@Service
@Slf4j
public class IpInfoService {

  private static final String LOOKUP_URI =
      "/json/{ipAddress}?fields=status,message,country,countryCode,region,regionName,city,zip,"
          + "lat,lon,timezone,isp,org,as,mobile,proxy,hosting,query";

  private final RestClient restClient = RestClient.builder().baseUrl("http://ip-api.com").build();

  @Cacheable(
      cacheNames = CacheNames.IP_INFO,
      key = "#ipAddress",
      unless = "!'success'.equals(#result.status())")
  public ViolatedUserIpInfo lookup(String ipAddress) {
    try {
      Map<String, Object> response =
          restClient
              .get()
              .uri(LOOKUP_URI, ipAddress)
              .retrieve()
              .body(new ParameterizedTypeReference<Map<String, Object>>() {});
      if (response == null) {
        return failed(ipAddress, "No IP information returned");
      }
      return toIpInfo(ipAddress, response);
    } catch (Exception ex) {
      log.warn("IP lookup failed for {}: {}", ipAddress, ex.getMessage());
      return failed(ipAddress, ex.getMessage());
    }
  }

  private static ViolatedUserIpInfo failed(String ipAddress, String message) {
    return new ViolatedUserIpInfo(
        ipAddress, "fail", message, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, Map.of());
  }

  private static ViolatedUserIpInfo toIpInfo(String ipAddress, Map<String, Object> response) {
    String responseIp = text(response.get("query"));
    return new ViolatedUserIpInfo(
        responseIp == null ? ipAddress : responseIp,
        text(response.get("status")),
        text(response.get("message")),
        text(response.get("country")),
        text(response.get("countryCode")),
        text(response.get("region")),
        text(response.get("regionName")),
        text(response.get("city")),
        text(response.get("zip")),
        decimal(response.get("lat")),
        decimal(response.get("lon")),
        text(response.get("timezone")),
        text(response.get("isp")),
        text(response.get("org")),
        text(response.get("as")),
        flag(response.get("mobile")),
        flag(response.get("proxy")),
        flag(response.get("hosting")),
        response);
  }

  private static String text(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private static Double decimal(Object value) {
    return value instanceof Number number ? number.doubleValue() : null;
  }

  private static Boolean flag(Object value) {
    return value instanceof Boolean bool ? bool : null;
  }
}
