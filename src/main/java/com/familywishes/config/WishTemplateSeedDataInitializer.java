package com.familywishes.config;

import com.familywishes.entity.WishSeedTemplate;
import com.familywishes.entity.enums.SeedTemplateType;
import com.familywishes.repository.WishSeedTemplateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WishTemplateSeedDataInitializer {

  private final WishSeedTemplateRepository wishSeedTemplateRepository;

  @PostConstruct
  public void initialize() {
    createIfMissing(SeedTemplateType.GOOD_MORNING, "Family", "Good Morning", "Emotional", "EN");
    createIfMissing(SeedTemplateType.FESTIVAL, "Family", "FESTIVAL", "Emotional", "EN");
  }

  private void createIfMissing(
      SeedTemplateType type, String relation, String event, String tone, String language) {
    if (wishSeedTemplateRepository.findByType(type).isPresent()) {
      return;
    }

    wishSeedTemplateRepository.save(
        WishSeedTemplate.builder()
            .type(type)
            .relation(relation)
            .event(event)
            .tone(tone)
            .language(language)
            .active(true)
            .build());
  }
}
