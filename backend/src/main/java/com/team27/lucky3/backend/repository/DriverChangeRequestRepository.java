package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.DriverChangeRequest;
import com.team27.lucky3.backend.entity.enums.DriverChangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverChangeRequestRepository
        extends JpaRepository<DriverChangeRequest, Long> {

    List<DriverChangeRequest> findByStatus(DriverChangeStatus status);
}
