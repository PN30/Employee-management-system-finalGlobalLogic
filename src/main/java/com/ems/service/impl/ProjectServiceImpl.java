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

/**
 * Provides project operations backed by the project and employee repositories.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Creates a project.
     *
     * @param request project details
     * @return the created project
     * @throws DuplicateResourceException if a project with the same name exists
     */
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

    /**
     * Updates an existing project.
     *
     * @param id project identifier
     * @param request updated project details
     * @return the updated project
     * @throws ResourceNotFoundException if the project does not exist
     * @throws DuplicateResourceException if another project has the requested name
     */
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

    /**
     * Deletes a project and detaches its employees.
     *
     * @param id project identifier
     * @throws ResourceNotFoundException if the project does not exist
     */
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

    /**
     * Retrieves a project by identifier.
     *
     * @param id project identifier
     * @return the requested project
     * @throws ResourceNotFoundException if the project does not exist
     */
    @Override
    public ProjectResponse getById(Long id) {
        return EntityMapper.toResponse(findProject(id));
    }

    /**
     * Retrieves all projects.
     *
     * @return all projects
     */
    @Override
    public List<ProjectResponse> getAll() {
        return toResponses(projectRepository.findAll());
    }

    /**
     * Assigns an employee to a project.
     *
     * @param projectId project identifier
     * @param employeeId employee identifier
     * @return the updated project
     * @throws ResourceNotFoundException if the project or employee does not exist
     * @throws DuplicateResourceException if the employee is already assigned
     */
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

    /**
     * Removes an employee's assignment to a project.
     *
     * @param projectId project identifier
     * @param employeeId employee identifier
     * @return the updated project
     * @throws ResourceNotFoundException if the project, employee, or assignment does not exist
     */
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

    /**
     * Retrieves employees assigned to a project.
     *
     * @param projectId project identifier
     * @return employees assigned to the project
     * @throws ResourceNotFoundException if the project does not exist
     */
    @Override
    public List<EmployeeResponse> getEmployeesByProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id " + projectId);
        }
        return employeeRepository.findByProjects_Id(projectId).stream().map(EntityMapper::toResponse).toList();
    }

    /**
     * Retrieves projects assigned to an employee.
     *
     * @param employeeId employee identifier
     * @return projects assigned to the employee
     * @throws ResourceNotFoundException if the employee does not exist
     */
    @Override
    public List<ProjectResponse> getProjectsByEmployee(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id " + employeeId);
        }
        return toResponses(projectRepository.findByEmployees_Id(employeeId));
    }

    /**
     * Retrieves projects whose end date is today or later, or has not been set.
     *
     * @return ongoing projects
     */
    @Override
    public List<ProjectResponse> getOngoingProjects() {
        LocalDate today = LocalDate.now();
        return toResponses(projectRepository.findAll().stream()
                .filter(p -> isOngoing(p, today))
                .toList());
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + id));
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }

    private static boolean isOngoing(Project project, LocalDate today) {
        return project.getEndDate() == null || !project.getEndDate().isBefore(today);
    }

    private List<ProjectResponse> toResponses(List<Project> projects) {
        return projects.stream().map(EntityMapper::toResponse).toList();
    }

    private void apply(Project project, ProjectRequest request, String name) {
        project.setName(name);
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
    }
}
