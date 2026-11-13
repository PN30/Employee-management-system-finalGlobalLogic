package com.ems.service;

import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;

import java.util.List;

public interface EmployeeService {
    EmployeeResponse create(EmployeeRequest request);
    EmployeeResponse update(Long id, EmployeeRequest request);
    void delete(Long id);
    EmployeeResponse getById(Long id);
    List<EmployeeResponse> getAll();
    List<EmployeeResponse> searchByName(String name);
    List<EmployeeResponse> getByDepartment(Long departmentId);
    List<EmployeeResponse> getActive();
    EmployeeResponse deactivate(Long id);
    long countByDepartment(Long departmentId);
}
