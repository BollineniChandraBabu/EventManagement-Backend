package com.familywishes.service.impl;

import com.familywishes.dto.FestivalDtos.FestivalResponse;
import com.familywishes.entity.SpecialEvent;
import com.familywishes.repository.SpecialEventRepository;
import com.familywishes.service.FestivalService;
import java.time.LocalDate;
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
  public List<FestivalResponse> listByMonth(Integer month) {
    return specialEventRepository.findByMonth(month).stream()
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

    try {
      List<Map<String, Object>> holidays = fetchHolidays(year);
      upsertFestivals(holidays, seenExternalIds);
    } catch (Exception ex) {
      log.error("Calendarific sync failed", ex);
    }

    List<SpecialEvent> allEvents = specialEventRepository.findAll();
    for (SpecialEvent event : allEvents) {
      if (event.getExternalEventId() != null
          && event.getEventDate() != null
          && event.getEventDate().getYear() == year
          && !seenExternalIds.contains(event.getExternalEventId())) {
        event.setActive(false);
      }
    }
    specialEventRepository.saveAll(allEvents);
  }

  private List<Map<String, Object>> fetchHolidays(int year) {
    String url =
        UriComponentsBuilder.fromHttpUrl(calendarificUrl)
            .queryParam("api_key", calendarificApiKey)
            .queryParam("country", calendarificCountry)
            .queryParam("year", year)
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

  private void upsertFestivals(List<Map<String, Object>> holidays, Set<String> seenExternalIds) {
    for (Map<String, Object> holiday : holidays) {
      String name = String.valueOf(holiday.getOrDefault("name", "Festival"));
      String urlId = String.valueOf(holiday.getOrDefault("urlid", ""));

      LocalDate eventDate = extractDate(holiday);
      if (eventDate == null) {
        continue;
      }

      String externalEventId = buildExternalEventId(urlId, eventDate, name);
      seenExternalIds.add(externalEventId);

      Optional<SpecialEvent> existing = specialEventRepository.findByExternalEventId(externalEventId);

      SpecialEvent specialEvent = existing.orElseGet(SpecialEvent::new);
      specialEvent.setExternalEventId(externalEventId);
      specialEvent.setEventName(name);
      specialEvent.setEventDate(eventDate);
      if (!StringUtils.hasText(specialEvent.getMessage())) {
        specialEvent.setMessage("Happy " + name + "! Wishing you joy and prosperity.");
      }
      specialEvent.setActive(true);
      specialEventRepository.save(specialEvent);
    }
  }

  private LocalDate extractDate(Map<String, Object> holiday) {
    Object dateNode = holiday.get("date");
    if (!(dateNode instanceof Map<?, ?> dateMap)) {
      return null;
    }

    Object isoNode = dateMap.get("iso");
    if (isoNode != null) {
      try {
        return LocalDate.parse(String.valueOf(isoNode));
      } catch (Exception ex) {
        return null;
      }
    }

    try {
      Object datetimeNode = dateMap.get("datetime");
      if (!(datetimeNode instanceof Map<?, ?> datetimeMap)) {
        return null;
      }

      int year = Integer.parseInt(String.valueOf(datetimeMap.get("year")));
      int month = Integer.parseInt(String.valueOf(datetimeMap.get("month")));
      int day = Integer.parseInt(String.valueOf(datetimeMap.get("day")));
      return LocalDate.of(year, month, day);
    } catch (Exception ex) {
      return null;
    }
  }

  private String buildExternalEventId(String urlId, LocalDate date, String name) {
    String slugSource = StringUtils.hasText(urlId) ? urlId : name;
    return date
        + "-"
        + slugSource.toLowerCase().replaceAll("[^a-z0-9/]+", "-").replace('/', '-');
  }

  private FestivalResponse toFestivalResponse(SpecialEvent event) {
    return new FestivalResponse(event.getId(), event.getEventName(), event.getEventDate(), event.isActive());
  }
}
