package com.familywishes.repository;

import com.familywishes.entity.EmailTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    List<EmailTemplate> findByNameOrderByVersionDesc(String name);

    @Query("""
            SELECT e FROM EmailTemplate e
            WHERE e.name = :name
              AND (
                    :searchKey = ''
                    OR LOWER(e.subject) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                    OR LOWER(e.htmlContent) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                  )
            """)
    Page<EmailTemplate> findVersionsByNameAndSearchKey(@Param("name") String name, @Param("searchKey") String searchKey, Pageable pageable);
}
