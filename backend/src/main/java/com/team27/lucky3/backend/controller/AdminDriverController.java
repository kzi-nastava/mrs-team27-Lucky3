package com.team27.lucky3.backend.controller;


import jakarta.validation.Valid;
import com.team27.lucky3.backend.dto.request.CreateDriver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
//@PreAuthorize("hasRole('ADMIN')")
public class AdminDriverController {

    @PostMapping("/admin/drivers")
    public ResponseEntity<CreateDriver> createDriver(
            @Valid @RequestBody CreateDriver requestDTO
    ) {

        // calls service, service call repository to save with status=pending_activation
        // Driver driver = driverService.createDriver(requestDTO);

        //email sending with initial password setup link

        // returnam response dto
        // return new ResponseEntity<>(new DriverCreatedDTO(driver), HttpStatus.CREATED);
        // TODO: proveriti da li je ovo dobro
        return new ResponseEntity<>(requestDTO, HttpStatus.CREATED);
    }
}
