package org.example.backend.controller;


import jakarta.validation.Valid;
import org.example.backend.dto.request.CreateDriverDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/drivers")
//@PreAuthorize("hasRole('ADMIN')")
public class AdminDriverController {

    @PostMapping
    public ResponseEntity<CreateDriverDTO> createDriver(
            @Valid @RequestBody CreateDriverDTO requestDTO
    ) {

        // calls service, service call repository to save
        // Driver driver = driverService.createDriver(requestDTO);

        //System.out.println("Creating driver: " + requestDTO);

        // returnam response dto
        // return new ResponseEntity<>(new DriverCreatedDTO(driver), HttpStatus.CREATED);
        return new ResponseEntity<>(requestDTO, HttpStatus.CREATED);
    }
}
