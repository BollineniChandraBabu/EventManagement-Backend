package com.familywishes.config;

import com.familywishes.entity.EventTypeSeed;
import com.familywishes.entity.RelationshipSeed;
import com.familywishes.entity.TemplateTypeSeed;
import com.familywishes.repository.EventTypeSeedRepository;
import com.familywishes.repository.RelationshipSeedRepository;
import com.familywishes.repository.TemplateTypeSeedRepository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WishTemplateSeedDataInitializer {

  private final RelationshipSeedRepository relationshipSeedRepository;
  private final EventTypeSeedRepository eventTypeSeedRepository;
  private final TemplateTypeSeedRepository templateTypeSeedRepository;

  @PostConstruct
  public void initialize() {
    List.of(
            "WIFE",
            "HUSBAND",
            "FRIEND",
            "MOTHER",
            "FATHER",
            "SISTER",
            "BROTHER",
            "SON",
            "DAUGHTER",
            "DAUGHTER_IN_LAW",
            "ADMIN")
        .forEach(code -> createRelationshipIfMissing(code, titleCase(code)));
    List.of("ANNIVERSARY", "ENGAGEMENT", "FESTIVAL")
        .forEach(code -> createEventTypeIfMissing(code, titleCase(code)));
    List.of("GOOD_MORNING", "FESTIVAL")
        .forEach(code -> createTemplateTypeIfMissing(code, titleCase(code)));
  }

  private void createRelationshipIfMissing(String code, String displayName) {
    if (relationshipSeedRepository.findByCode(code).isPresent()) return;
    relationshipSeedRepository.save(
        RelationshipSeed.builder().code(code).displayName(displayName).active(true).build());
  }

  private void createEventTypeIfMissing(String code, String displayName) {
    if (eventTypeSeedRepository.findByCode(code).isPresent()) return;
    eventTypeSeedRepository.save(
        EventTypeSeed.builder().code(code).displayName(displayName).active(true).build());
  }

  private void createTemplateTypeIfMissing(String code, String displayName) {
    if (templateTypeSeedRepository.findByCode(code).isPresent()) return;
    templateTypeSeedRepository.save(
        TemplateTypeSeed.builder().code(code).displayName(displayName).active(true).build());
  }

  private String titleCase(String code) {
    return code.toLowerCase().replace('_', ' ');
  }
}
