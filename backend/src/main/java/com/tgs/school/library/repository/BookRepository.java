package com.tgs.school.library.repository;

import com.tgs.school.library.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query("SELECT b FROM Book b WHERE (:q IS NULL OR :q = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(COALESCE(b.isbn,'')) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "AND (:category IS NULL OR :category = '' OR LOWER(b.category) = LOWER(:category))")
    Page<Book> search(@Param("q") String q, @Param("category") String category, Pageable pageable);
}
