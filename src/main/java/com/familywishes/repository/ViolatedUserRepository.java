package com.familywishes.repository;

import com.familywishes.entity.User;
import com.familywishes.entity.ViolatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ViolatedUserRepository extends JpaRepository<ViolatedUser, Long> {
  List<ViolatedUser> findByEmail(String email);
}
