package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.response.ReportResponse;
import com.team27.lucky3.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    @Override
    public ReportResponse generateReportForUser(Long userId, LocalDateTime from, LocalDateTime to, String type) {
        // Implementation for generating user-specific report
        return null; // Placeholder return
    }

    @Override
    public ReportResponse generateGlobalReport(LocalDateTime from, LocalDateTime to, String type) {
        // Implementation for generating global report
        return null; // Placeholder return
    }
}
