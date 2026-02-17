package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.response.DailyReport;
import com.team27.lucky3.backend.dto.response.ReportResponse;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.repository.RideRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;

    @Override
    public ReportResponse generateReportForUser(Long userId, LocalDateTime from, LocalDateTime to, String type) {
        if(from == null || to == null || type == null){
            throw new IllegalArgumentException("From, To and Type parameters are required");
        }
        if(from.isAfter(to)){
            throw new IllegalArgumentException("From date cannot be after To date");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check user role and generate appropriate report
        if (user.getRole() == UserRole.DRIVER) {
            return generateDriverReport(userId, from, to);
        } else {
            return generatePassengerReport(userId, from, to);
        }
    }

    @Override
    public ReportResponse generateGlobalReport(LocalDateTime from, LocalDateTime to, String type, Long userId) {
        if(from == null || to == null || type == null){
            throw new IllegalArgumentException("From, To and Type parameters are required");
        }
        if(from.isAfter(to)){
            throw new IllegalArgumentException("From date cannot be after To date");
        }
        if ("DRIVER".equalsIgnoreCase(type)) {
            return generateAllDriversReport(from, to);
        } else if ("PASSENGER".equalsIgnoreCase(type)) {
            return generateAllPassengersReport(from, to);
        }
        else{
            return generateReportForUser(userId, from, to, type);
        }
    }

    private ReportResponse generateDriverReport(Long driverId, LocalDateTime from, LocalDateTime to) {
        // Get all completed rides for this driver in date range
        List<Ride> rides = rideRepository.findByDriverIdAndStartTimeBetweenAndStatus(
                driverId, from, to, RideStatus.FINISHED);

        return buildReportResponse(rides, from, to, true);
    }

    private ReportResponse generatePassengerReport(Long passengerId, LocalDateTime from, LocalDateTime to) {
        // Get all rides where user was a passenger
        List<Ride> rides = rideRepository.findRidesForPassenger(passengerId, from, to, RideStatus.FINISHED.name());
        return buildReportResponse(rides, from, to, false);
    }

    private ReportResponse generateAllDriversReport(LocalDateTime from, LocalDateTime to) {
        List<Ride> rides = rideRepository.findByStartTimeBetweenAndStatus(from, to, RideStatus.FINISHED);
        return buildReportResponse(rides, from, to, true);
    }

    private ReportResponse generateAllPassengersReport(LocalDateTime from, LocalDateTime to) {
        List<Ride> rides = rideRepository.findByStartTimeBetweenAndStatus(from, to, RideStatus.FINISHED);
        return buildReportResponse(rides, from, to, false);
    }

    private ReportResponse buildReportResponse(List<Ride> rides, LocalDateTime from, LocalDateTime to, boolean isDriver) {
        // Group rides by day
        Map<LocalDate, List<Ride>> ridesByDay = rides.stream()
                .collect(Collectors.groupingBy(ride -> ride.getStartTime().toLocalDate()));

        // Generate daily reports
        List<DailyReport> dailyData = new ArrayList<>();
        LocalDate currentDate = from.toLocalDate();
        LocalDate endDate = to.toLocalDate();

        double totalRides = 0;
        double totalKilometers = 0;
        double totalMoney = 0;

        while (!currentDate.isAfter(endDate)) {
            List<Ride> dayRides = ridesByDay.getOrDefault(currentDate, Collections.emptyList());

            int rideCount = dayRides.size();
            double kilometers = dayRides.stream()
                    .mapToDouble(r -> r.getDistanceTraveled() != null ? r.getDistanceTraveled() : 0.0)
                    .sum();
            double money = dayRides.stream()
                    .mapToDouble(r -> r.getTotalCost() != null ? r.getTotalCost() : 0.0)
                    .sum();

            DailyReport dailyReport = new DailyReport(currentDate.toString(), rideCount, kilometers, money);

            dailyData.add(dailyReport);

            totalRides += rideCount;
            totalKilometers += kilometers;
            totalMoney += money;

            currentDate = currentDate.plusDays(1);
        }

        // Calculate averages
        long dayCount = java.time.temporal.ChronoUnit.DAYS.between(from.toLocalDate(), to.toLocalDate()) + 1;

        int pendingRides = rideRepository.findByStartTimeBetweenAndStatus(from, to, RideStatus.PENDING).size();
        int activeRides = rideRepository.findByStartTimeBetweenAndStatus(from, to, RideStatus.ACTIVE).size();
        int inProgressRides = rideRepository.findByStartTimeBetweenAndStatus(from, to, RideStatus.IN_PROGRESS).size();
        int finishedRides = rideRepository.findByStartTimeBetweenAndStatus(from, to, RideStatus.FINISHED).size();
        int rejectedRides = rideRepository.findByStartTimeBetweenAndStatus(from, to, RideStatus.REJECTED).size();
        int panicRides = rideRepository.findByStartTimeBetweenAndStatus(from, to, RideStatus.PANIC).size();
        int cancelledRides = rideRepository.findByStartTimeBetweenAndStatus(from, to, RideStatus.CANCELLED).size() +
                rideRepository.findByStartTimeBetweenAndStatus(from, to, RideStatus.CANCELLED_BY_DRIVER).size() +
                rideRepository.findByStartTimeBetweenAndStatus(from, to, RideStatus.CANCELLED_BY_PASSENGER).size();

        return new ReportResponse(dailyData, totalRides, totalKilometers, totalMoney, totalRides / dayCount,
                totalKilometers / dayCount, totalMoney / dayCount, pendingRides, activeRides, inProgressRides, finishedRides, rejectedRides, panicRides, cancelledRides);
    }
}

