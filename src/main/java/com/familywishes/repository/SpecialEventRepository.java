package com.familywishes.repository;

import com.familywishes.entity.SpecialEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialEventRepository extends JpaRepository<SpecialEvent, Long> {

  List<SpecialEvent> findByDayAndMonthAndActiveTrue(int day, int month);
}
