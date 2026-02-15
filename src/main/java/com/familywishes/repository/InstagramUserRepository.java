package com.familywishes.repository;

import com.familywishes.entity.InstagramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InstagramUserRepository extends JpaRepository<InstagramUser, Long> {

    @Query("SELECT u FROM InstagramUser u WHERE MONTH(u.birthday)=:month AND DAY(u.birthday)=:day")
    List<InstagramUser> findTodaysBirthdays(int month, int day);
}
