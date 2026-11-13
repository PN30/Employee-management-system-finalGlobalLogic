package com.ems.mapper;

import com.ems.dto.DepartmentResponse;
import com.ems.dto.EmployeeResponse;
import com.ems.dto.ProjectResponse;
import com.ems.model.Department;
import com.ems.model.Employee;
import com.ems.model.Project;

public final class EntityMapper {

    private EntityMapper() {
    }

    public static EmployeeResponse toResponse(Employee e) {
        return new EmployeeResponse(
                e.getId(),
                e.getFirstName(),
                e.getLastName(),
                e.getFirstName() + " " + e.getLastName(),
                e.getEmail(),
                e.getJobTitle(),
                e.getSalary(),
                e.getHireDate(),
                e.isActive(),
                e.getDepartment().getId(),
                e.getDepartment().getName());
    }

    public static DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(d.getId(), d.getName(), d.getDescription(), d.getEmployees().size());
    }

    public static ProjectResponse toResponse(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(),
                p.getStartDate(), p.getEndDate(), p.getEmployees().size());
    }
}
