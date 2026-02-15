package com.familywishes.repository;

import com.familywishes.entity.SpecialEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecialEventRepository extends JpaRepository<SpecialEvent, Long> {

    List<SpecialEvent> findByDayAndMonthAndActiveTrue(int day, int month);
}