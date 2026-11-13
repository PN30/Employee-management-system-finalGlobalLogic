package com.ems.service;

import com.ems.dto.DepartmentRequest;
import com.ems.dto.DepartmentResponse;

import java.util.List;

public interface DepartmentService {
    DepartmentResponse create(DepartmentRequest request);
    DepartmentResponse update(Long id, DepartmentRequest request);
    void delete(Long id);
    DepartmentResponse getById(Long id);
    List<DepartmentResponse> getAll();
    DepartmentResponse getByName(String name);
}
