package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.request.CreateDriverRequest;
import com.team27.lucky3.backend.entity.DriverChangeRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface DriverChangeRequestService {
    @Transactional
    DriverChangeRequest createChangeRequest(Long driverId,
                                            CreateDriverRequest request,
                                            MultipartFile profileImage) throws IOException;
}
