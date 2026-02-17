package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.response.ReportResponse;
import java.time.LocalDateTime;

public interface ReportService {
    ReportResponse generateReportForUser(Long userId, LocalDateTime from, LocalDateTime to);
    ReportResponse generateGlobalReport(LocalDateTime from, LocalDateTime to, String type);
}