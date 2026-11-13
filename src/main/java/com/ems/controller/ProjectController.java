package com.ems.controller;

import com.ems.dto.EmployeeResponse;
import com.ems.dto.ProjectRequest;
import com.ems.dto.ProjectResponse;
import com.ems.service.ProjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody ProjectRequest request) {
        return projectService.create(request);
    }

    @GetMapping
    public List<ProjectResponse> getAll() {
        return projectService.getAll();
    }

    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable Long id) {
        return projectService.getById(id);
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        projectService.delete(id);
    }

    @PostMapping("/{projectId}/employees/{employeeId}")
    public ProjectResponse assignEmployee(@PathVariable Long projectId, @PathVariable Long employeeId) {
        return projectService.assignEmployee(projectId, employeeId);
    }

    @DeleteMapping("/{projectId}/employees/{employeeId}")
    public ProjectResponse removeEmployee(@PathVariable Long projectId, @PathVariable Long employeeId) {
        return projectService.removeEmployee(projectId, employeeId);
    }

    @GetMapping("/{projectId}/employees")
    public List<EmployeeResponse> getEmployees(@PathVariable Long projectId) {
        return projectService.getEmployeesByProject(projectId);
    }
}
