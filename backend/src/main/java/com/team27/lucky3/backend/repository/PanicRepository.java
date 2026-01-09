package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.Panic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PanicRepository extends JpaRepository<Panic, Long> {
}

