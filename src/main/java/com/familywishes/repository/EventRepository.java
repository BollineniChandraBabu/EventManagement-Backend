package com.familywishes.repository;

import com.familywishes.entity.Event;
import com.familywishes.entity.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByEventDateAndActiveTrue(LocalDate eventDate);

    long countByEventDateGreaterThanEqualAndActiveTrue(LocalDate eventDate);

    List<Event> findByEventTypeInAndActiveTrue(List<EventType> eventTypes);
}
