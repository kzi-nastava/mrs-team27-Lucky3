package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.request.CreateDriverRequest;
import com.team27.lucky3.backend.entity.DriverChangeRequest;
import com.team27.lucky3.backend.entity.Image;
import com.team27.lucky3.backend.entity.enums.DriverChangeStatus;
import com.team27.lucky3.backend.repository.DriverChangeRequestRepository;
import com.team27.lucky3.backend.repository.ImageRepository;
import com.team27.lucky3.backend.service.DriverChangeRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DriverChangeRequestServiceImpl implements DriverChangeRequestService {
    private final ImageRepository imageRepository;
    private final DriverChangeRequestRepository changeRepo;

    @Transactional
    public DriverChangeRequest createChangeRequest(Long driverId,
                                    CreateDriverRequest request,
                                    MultipartFile profileImage) throws IOException {

        DriverChangeRequest cr = new DriverChangeRequest();
        cr.setName(request.getName());
        cr.setDriverRequestId(driverId);
        cr.setSurname(request.getSurname());
        cr.setEmail(request.getEmail());
        cr.setAddress(request.getAddress());
        cr.setPhone(request.getPhone());
        cr.setVehicle(request.getVehicle());
        cr.setStatus(DriverChangeStatus.PENDING);
        cr.setCreatedAt(LocalDateTime.now());

        // handle image (e.g. store and reference id/url)
        if (profileImage != null && !profileImage.isEmpty()) {
            Image image = new Image();
            image.setFileName(profileImage.getOriginalFilename());
            image.setContentType(profileImage.getContentType());
            image.setSize(profileImage.getSize());
            image.setData(profileImage.getBytes());

            Image saved = imageRepository.save(image);
            Long imageId = saved.getId();

            // e.g. store on change request:
            cr.setImageId(imageId);
        } else {
            cr.setImageId(null); // or handle as needed
        }

        changeRepo.save(cr);
        return cr;
    }
}
