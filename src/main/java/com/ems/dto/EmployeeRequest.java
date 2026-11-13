package com.ems.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 100) String jobTitle,
        @PositiveOrZero BigDecimal salary,
        @NotNull LocalDate hireDate,
        Boolean active,
        @NotNull Long departmentId) {
}
