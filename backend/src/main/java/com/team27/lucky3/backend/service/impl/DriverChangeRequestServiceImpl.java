package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.request.CreateDriverRequest;
import com.team27.lucky3.backend.dto.request.ReviewDriverChange;
import com.team27.lucky3.backend.dto.request.VehicleInformation;
import com.team27.lucky3.backend.entity.DriverChangeRequest;
import com.team27.lucky3.backend.entity.Image;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.Vehicle;
import com.team27.lucky3.backend.entity.enums.DriverChangeStatus;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.DriverChangeRequestRepository;
import com.team27.lucky3.backend.repository.ImageRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.repository.VehicleRepository;
import com.team27.lucky3.backend.service.DriverChangeRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Driver;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DriverChangeRequestServiceImpl implements DriverChangeRequestService {
    private final ImageRepository imageRepository;
    private final DriverChangeRequestRepository changeRepo;
    private final UserRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public DriverChangeRequest createChangeRequest(Long driverId,
                                    CreateDriverRequest request,
                                    MultipartFile profileImage) throws IOException {

        DriverChangeRequest cr = new DriverChangeRequest();
        cr.setName(request.getName());
        cr.setRequestedDriverId(driverId);
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

    public List<DriverChangeRequest> getChangeRequests(DriverChangeStatus status) {
        if (status != null) {
            return changeRepo.findByStatus(status);
        } else {
            return changeRepo.findAll();
        }
    }

    @Override
    @Transactional
    public void reviewChangeRequest(Long requestId, ReviewDriverChange review) {
        DriverChangeRequest cr = changeRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Change request not found"));

        if (review.isApprove()) {
            cr.setStatus(DriverChangeStatus.APPROVED);

            //This part updates the driver information (as user)
            User driver = driverRepository
                    .findByIdAndRole(cr.getRequestedDriverId(), UserRole.DRIVER)
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

            driver.setName(cr.getName());
            driver.setSurname(cr.getSurname());
            driver.setEmail(cr.getEmail());
            driver.setAddress(cr.getAddress());
            driver.setPhoneNumber(cr.getPhone());

            //update profile image if you store imageId/url
            if(cr.getImageId() != null) {
                Image img = imageRepository.findById(cr.getImageId())
                        .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
                driver.setProfileImage(img);
            }
            driverRepository.save(driver);

            //This part updates the vehicle information
            Vehicle vehicle = vehicleRepository
                    .findByDriverId(driver.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle for driver not found"));

            VehicleInformation vehicleInfo = cr.getVehicle();
            vehicle.setSeatCount(vehicleInfo.getPassengerSeats());
            vehicle.setLicensePlates(vehicleInfo.getLicenseNumber());
            vehicle.setModel(vehicleInfo.getModel());
            vehicle.setVehicleType(vehicleInfo.getVehicleType());
            vehicle.setBabyTransport(vehicleInfo.getBabyTransport());
            vehicle.setPetTransport(vehicleInfo.getPetTransport());

            vehicleRepository.save(vehicle);
        } else {
            cr.setStatus(DriverChangeStatus.REJECTED);
        }
        changeRepo.delete(cr);
    }
}
