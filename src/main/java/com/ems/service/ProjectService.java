package com.ems.service;

import com.ems.dto.EmployeeResponse;
import com.ems.dto.ProjectRequest;
import com.ems.dto.ProjectResponse;

import java.util.List;

public interface ProjectService {
    ProjectResponse create(ProjectRequest request);
    ProjectResponse update(Long id, ProjectRequest request);
    void delete(Long id);
    ProjectResponse getById(Long id);
    List<ProjectResponse> getAll();
    ProjectResponse assignEmployee(Long projectId, Long employeeId);
    ProjectResponse removeEmployee(Long projectId, Long employeeId);
    List<EmployeeResponse> getEmployeesByProject(Long projectId);
    List<ProjectResponse> getProjectsByEmployee(Long employeeId);
    List<ProjectResponse> getOngoingProjects();
}
