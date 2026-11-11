package com.familywishes.dto;

import java.time.LocalDate;

public class CalendarDtos {

  /**
   * @param recurring false = happens only on eventDate (festivals, one-time events); true = repeats
   *     every year (birthdays, recurring events)
   * @param ownerName the person the entry belongs to (events and birthdays), null for festivals
   * @param ownerUserId the id of the person the entry belongs to, null for festivals
   * @param fromCircle true when the entry belongs to a circle member rather than the signed-in user
   *     (or, for an admin, an entry that is not the admin's own)
   */
  public record CalendarResponse(
      Long id,
      String eventType,
      String eventName,
      LocalDate eventDate,
      boolean active,
      boolean recurring,
      String ownerName,
      Long ownerUserId,
      boolean fromCircle) {}
}
