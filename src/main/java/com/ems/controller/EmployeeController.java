package com.ems.controller;

import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;
import com.ems.dto.ProjectResponse;
import com.ems.service.EmployeeService;
import com.ems.service.ProjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Tag(name = "Employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final ProjectService projectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody EmployeeRequest request) {
        return employeeService.create(request);
    }

    @GetMapping
    public List<EmployeeResponse> getAll() {
        return employeeService.getAll();
    }

    @GetMapping("/search")
    public List<EmployeeResponse> search(@RequestParam String name) {
        return employeeService.searchByName(name);
    }

    @GetMapping("/active")
    public List<EmployeeResponse> getActive() {
        return employeeService.getActive();
    }

    @GetMapping("/department/{departmentId}")
    public List<EmployeeResponse> getByDepartment(@PathVariable Long departmentId) {
        return employeeService.getByDepartment(departmentId);
    }

    @GetMapping("/{id}")
    public EmployeeResponse getById(@PathVariable Long id) {
        return employeeService.getById(id);
    }

    @GetMapping("/{id}/projects")
    public List<ProjectResponse> getProjects(@PathVariable Long id) {
        return projectService.getProjectsByEmployee(id);
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return employeeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        employeeService.delete(id);
    }
}
