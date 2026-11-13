package com.ems.service.impl;

import com.ems.dto.EmployeeResponse;
import com.ems.dto.ProjectRequest;
import com.ems.dto.ProjectResponse;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.mapper.EntityMapper;
import com.ems.model.Employee;
import com.ems.model.Project;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.ProjectRepository;
import com.ems.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        String name = request.name().trim();
        if (projectRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Project '" + name + "' already exists");
        }
        Project project = new Project();
        apply(project, request, name);
        Project saved = projectRepository.save(project);
        log.info("Created project id={} name={}", saved.getId(), saved.getName());
        return EntityMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = findProject(id);
        String name = request.name().trim();
        if (projectRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Project '" + name + "' already exists");
        }
        apply(project, request, name);
        log.info("Updated project id={}", id);
        return EntityMapper.toResponse(project);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Project project = findProject(id);
        // Employee owns the join table, so detach from that side before deleting.
        new ArrayList<>(project.getEmployees()).forEach(e -> e.getProjects().remove(project));
        project.getEmployees().clear();
        projectRepository.delete(project);
        log.info("Deleted project id={}", id);
    }

    @Override
    public ProjectResponse getById(Long id) {
        return EntityMapper.toResponse(findProject(id));
    }

    @Override
    public List<ProjectResponse> getAll() {
        return projectRepository.findAll().stream().map(EntityMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public ProjectResponse assignEmployee(Long projectId, Long employeeId) {
        Project project = findProject(projectId);
        Employee employee = findEmployee(employeeId);
        if (employee.getProjects().contains(project)) {
            throw new DuplicateResourceException("Employee is already assigned to this project");
        }
        employee.getProjects().add(project);
        project.getEmployees().add(employee);
        log.info("Assigned employee {} to project {}", employeeId, projectId);
        return EntityMapper.toResponse(project);
    }

    @Override
    @Transactional
    public ProjectResponse removeEmployee(Long projectId, Long employeeId) {
        Project project = findProject(projectId);
        Employee employee = findEmployee(employeeId);
        if (!employee.getProjects().contains(project)) {
            throw new ResourceNotFoundException("Employee is not assigned to this project");
        }
        employee.getProjects().remove(project);
        project.getEmployees().remove(employee);
        log.info("Removed employee {} from project {}", employeeId, projectId);
        return EntityMapper.toResponse(project);
    }

    @Override
    public List<EmployeeResponse> getEmployeesByProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id " + projectId);
        }
        return employeeRepository.findByProjects_Id(projectId).stream().map(EntityMapper::toResponse).toList();
    }

    @Override
    public List<ProjectResponse> getProjectsByEmployee(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id " + employeeId);
        }
        return projectRepository.findByEmployees_Id(employeeId).stream().map(EntityMapper::toResponse).toList();
    }

    @Override
    public List<ProjectResponse> getOngoingProjects() {
        LocalDate today = LocalDate.now();
        return projectRepository.findAll().stream()
                .filter(p -> p.getEndDate() == null || !p.getEndDate().isBefore(today))
                .map(EntityMapper::toResponse)
                .toList();
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + id));
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }

    private void apply(Project project, ProjectRequest request, String name) {
        project.setName(name);
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
    }
}
