package com.ems.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String jobTitle,
        BigDecimal salary,
        LocalDate hireDate,
        boolean active,
        Long departmentId,
        String departmentName) {
}
