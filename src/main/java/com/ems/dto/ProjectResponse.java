package com.ems.dto;

import java.time.LocalDate;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        int employeeCount) {
}
