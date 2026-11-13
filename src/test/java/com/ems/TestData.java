package com.ems;

import com.ems.model.Department;
import com.ems.model.Employee;
import com.ems.model.Project;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class TestData {

    private TestData() {
    }

    public static Department department(String name) {
        Department d = new Department();
        d.setName(name);
        d.setDescription(name + " team");
        return d;
    }

    public static Employee employee(String first, String last, String email, Department department, boolean active) {
        Employee e = new Employee();
        e.setFirstName(first);
        e.setLastName(last);
        e.setEmail(email);
        e.setJobTitle("Engineer");
        e.setSalary(new BigDecimal("50000.00"));
        e.setHireDate(LocalDate.of(2024, 1, 15));
        e.setActive(active);
        e.setDepartment(department);
        return e;
    }

    public static Project project(String name) {
        Project p = new Project();
        p.setName(name);
        p.setDescription(name + " project");
        p.setStartDate(LocalDate.of(2024, 1, 1));
        p.setEndDate(LocalDate.of(2024, 12, 31));
        return p;
    }
}
