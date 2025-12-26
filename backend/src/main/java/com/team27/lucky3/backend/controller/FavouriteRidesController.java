package com.team27.lucky3.backend.controller;
import com.team27.lucky3.backend.dto.response.RideResponse;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/favourite-rides")
@RequiredArgsConstructor
public class FavouriteRidesController {
    @PostMapping("/{favouriteRideId}/order")
    public ResponseEntity<RideResponse> orderFromFavouriteRide(
            @PathVariable Long favouriteRideId,
            Authentication auth
    ){
        //U sustini, ja dobijem id omiljenje voznje, i auth. Pogledam u bazi da li je taj
        //user vlasnik te omiljene voznje, ako jeste, kreiram novu voznju sa podacima iz omiljene voznje
        //i vracam je kao odgovor. Ovde je sve to mockovano.
        RideResponse mocked = new RideResponse();
        mocked.setId(12L);
        mocked.setStartTime(LocalDateTime.of(2025, 12, 26, 19, 30));
        mocked.setEndTime(LocalDateTime.of(2025, 12, 26, 19, 50));
        mocked.setTotalCost(1280.0);
        mocked.setDriverEmail("driver1@gmail.com");
        mocked.setPassengerEmail("passenger1@gmail.com");
        mocked.setEstimatedTimeInMinutes(20L);
        mocked.setRideStatus("ASSIGNED");
        mocked.setRejectionReason(null);
        mocked.setPanicPressed(false);
        mocked.setDeparture("Bulevar Oslobodjenja 1");
        mocked.setDestination("Promenada Novi Sad");

        return ResponseEntity.status(HttpStatus.CREATED).body(mocked);
    }
}
