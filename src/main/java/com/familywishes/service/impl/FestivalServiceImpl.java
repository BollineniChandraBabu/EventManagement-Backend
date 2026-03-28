package com.familywishes.service.impl;

import com.familywishes.dto.FestivalDtos.FestivalResponse;
import com.familywishes.entity.SpecialEvent;
import com.familywishes.repository.SpecialEventRepository;
import com.familywishes.service.FestivalService;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class FestivalServiceImpl implements FestivalService {

  private final RestTemplate restTemplate;
  private final SpecialEventRepository specialEventRepository;

  @Value("${calendarific.api.url:https://calendarific.com/api/v2/holidays}")
  private String calendarificUrl;

  @Value("${calendarific.api.key:}")
  private String calendarificApiKey;

  @Value("${calendarific.country:IN}")
  private String calendarificCountry;

  @Override
  public List<FestivalResponse> listByMonth(int month) {
    return specialEventRepository.findByMonthOrderByDayAscEventNameAsc(month).stream()
        .map(this::toFestivalResponse)
        .toList();
  }

  @Override
  public void syncCalendarificFestivals() {
    if (!StringUtils.hasText(calendarificApiKey)) {
      log.warn("Skipping Calendarific sync because calendarific.api.key is missing");
      return;
    }

    int year = Year.now().getValue();
    Set<String> seenExternalIds = new HashSet<>();

    for (int month = 1; month <= 12; month++) {
      try {
        List<Map<String, Object>> holidays = fetchHolidays(year, month);
        upsertFestivals(year, month, holidays, seenExternalIds);
      } catch (Exception ex) {
        log.error("Calendarific sync failed for month {}", month, ex);
      }
    }

    List<SpecialEvent> allEvents = specialEventRepository.findAll();
    for (SpecialEvent event : allEvents) {
      if (event.getExternalEventId() != null
          && event.getYear() != null
          && event.getYear() == year
          && !seenExternalIds.contains(event.getExternalEventId())) {
        event.setActive(false);
      }
    }
    specialEventRepository.saveAll(allEvents);
  }

  private List<Map<String, Object>> fetchHolidays(int year, int month) {
    String url =
        UriComponentsBuilder.fromHttpUrl(calendarificUrl)
            .queryParam("api_key", calendarificApiKey)
            .queryParam("country", calendarificCountry)
            .queryParam("year", year)
            .queryParam("month", month)
            .toUriString();

    ResponseEntity<Map<String, Object>> response =
        restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {});

    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      return List.of();
    }

    Map<String, Object> meta = response.getBody();
    Object responseNode = meta.get("response");
    if (!(responseNode instanceof Map<?, ?> responseMap)) {
      return List.of();
    }

    Object holidays = responseMap.get("holidays");
    if (!(holidays instanceof List<?> holidayList)) {
      return List.of();
    }

    List<Map<String, Object>> result = new ArrayList<>();
    for (Object holiday : holidayList) {
      if (holiday instanceof Map<?, ?> holidayMap) {
        @SuppressWarnings("unchecked")
        Map<String, Object> typedHoliday = (Map<String, Object>) holidayMap;
        result.add(typedHoliday);
      }
    }

    return result;
  }

  private void upsertFestivals(
      int year, int month, List<Map<String, Object>> holidays, Set<String> seenExternalIds) {
    for (Map<String, Object> holiday : holidays) {
      String name = String.valueOf(holiday.getOrDefault("name", "Festival"));

      Integer day = extractDay(holiday);
      if (day == null) {
        continue;
      }

      String externalEventId = buildExternalEventId(year, month, day, name);
      seenExternalIds.add(externalEventId);

      Optional<SpecialEvent> existing = specialEventRepository.findByExternalEventId(externalEventId);

      SpecialEvent specialEvent = existing.orElseGet(SpecialEvent::new);
      specialEvent.setExternalEventId(externalEventId);
      specialEvent.setEventName(name);
      specialEvent.setDay(day);
      specialEvent.setMonth(month);
      specialEvent.setYear(year);
      if (!StringUtils.hasText(specialEvent.getMessage())) {
        specialEvent.setMessage("Happy " + name + "! Wishing you joy and prosperity.");
      }
      specialEvent.setActive(true);
      specialEventRepository.save(specialEvent);
    }
  }

  private Integer extractDay(Map<String, Object> holiday) {
    Object dateNode = holiday.get("date");
    if (!(dateNode instanceof Map<?, ?> dateMap)) {
      return null;
    }

    Object datetimeNode = dateMap.get("datetime");
    if (!(datetimeNode instanceof Map<?, ?> datetimeMap)) {
      return null;
    }

    Object dayNode = datetimeMap.get("day");
    if (dayNode instanceof Number n) {
      return n.intValue();
    }

    try {
      return Integer.parseInt(String.valueOf(dayNode));
    } catch (Exception ex) {
      return null;
    }
  }

  private String buildExternalEventId(int year, int month, int day, String name) {
    return year
        + "-"
        + month
        + "-"
        + day
        + "-"
        + name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
  }

  private FestivalResponse toFestivalResponse(SpecialEvent event) {
    return new FestivalResponse(
        event.getId(), event.getEventName(), event.getDay(), event.getMonth(), event.getYear(), event.isActive());
  }
}
