package com.ems.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProjectRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        LocalDate startDate,
        LocalDate endDate) {

    @AssertTrue(message = "endDate must not be before startDate")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
