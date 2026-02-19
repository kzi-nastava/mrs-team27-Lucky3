package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.Panic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PanicRepository extends JpaRepository<Panic, Long> {
    Page<Panic> findAllByOrderByTimestampDesc(Pageable pageable);
    List<Panic> findAllByOrderByTimestampDesc();
}

