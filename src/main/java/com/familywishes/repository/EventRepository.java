package com.familywishes.repository;

import com.familywishes.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByEventDateAndActiveTrue(LocalDate eventDate);
}
