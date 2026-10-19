package com.familywishes.config;

/**
 * Names of the Ehcache caches. Every name here must have a matching {@code <cache alias="...">} in
 * {@code ehcache.xml} (CacheConfigurationTest checks this), otherwise Spring fails at the first use.
 */
public final class CacheNames {

  /** Hibernate second-level cache region shared by the small reference-data entities. */
  public static final String SEED_DATA = "seedData";

  /** Email images downloaded from storage, by email log id. */
  public static final String EMAIL_IMAGES = "emailImages";

  /** IP geolocation lookups from ip-api.com, by IP address. */
  public static final String IP_INFO = "ipInfo";

  /** Festivals of a month (shared by every user of the calendar). */
  public static final String FESTIVALS_BY_MONTH = "festivalsByMonth";

  /** Dashboard counters; short lived on purpose. */
  public static final String DASHBOARD_SUMMARY = "dashboardSummary";

  private CacheNames() {}
}
