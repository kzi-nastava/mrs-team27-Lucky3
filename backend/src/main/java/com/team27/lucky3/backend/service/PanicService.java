package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.response.PanicResponse;
import com.team27.lucky3.backend.entity.Panic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PanicService {
    Page<PanicResponse> findAll(Pageable pageable);

    /**
     * Build a PanicResponse from a Panic entity and broadcast it to admins via WebSocket.
     * Called after a panic event is persisted by RideService.
     *
     * @param panic The persisted Panic entity
     */
    void broadcastPanicAlert(Panic panic);
}