package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.response.PanicResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PanicService {
    Page<PanicResponse> findAll(Pageable pageable);
}