package com.ems.service.impl;

import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.mapper.EntityMapper;
import com.ems.model.Department;
import com.ems.model.Employee;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import com.ems.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Provides employee operations backed by the employee and department repositories.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * Creates an employee from the supplied request.
     *
     * @param request employee details
     * @return the created employee
     * @throws DuplicateResourceException if the email is already in use
     */
    @Override
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        String email = normalizeEmail(request.email());
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An employee with email " + email + " already exists");
        }
        Employee employee = new Employee();
        apply(employee, request, email);
        Employee saved = employeeRepository.save(employee);
        log.info("Created employee id={} email={}", saved.getId(), saved.getEmail());
        return EntityMapper.toResponse(saved);
    }

    /**
     * Updates an existing employee.
     *
     * @param id employee identifier
     * @param request updated employee details
     * @return the updated employee
     * @throws ResourceNotFoundException if the employee or requested department does not exist
     * @throws DuplicateResourceException if the email is already in use by another employee
     */
    @Override
    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = find(id);
        String email = normalizeEmail(request.email());
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateResourceException("An employee with email " + email + " already exists");
        }
        apply(employee, request, email);
        log.info("Updated employee id={}", id);
        return EntityMapper.toResponse(employee);
    }

    /**
     * Deletes an employee.
     *
     * @param id employee identifier
     * @throws ResourceNotFoundException if the employee does not exist
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Employee employee = find(id);
        employeeRepository.delete(employee);
        log.info("Deleted employee id={}", id);
    }

    /**
     * Retrieves an employee by identifier.
     *
     * @param id employee identifier
     * @return the requested employee
     * @throws ResourceNotFoundException if the employee does not exist
     */
    @Override
    public EmployeeResponse getById(Long id) {
        return EntityMapper.toResponse(find(id));
    }

    /**
     * Retrieves all employees.
     *
     * @return all employees
     */
    @Override
    public List<EmployeeResponse> getAll() {
        return toResponses(employeeRepository.findAll());
    }

    /**
     * Searches employees by name, or returns all employees for a blank query.
     *
     * @param name name to search for
     * @return employees matching the query
     */
    @Override
    public List<EmployeeResponse> searchByName(String name) {
        if (name == null || name.isBlank()) {
            return getAll();
        }
        return toResponses(employeeRepository.searchByName(name.trim()));
    }

    /**
     * Retrieves employees in a department.
     *
     * @param departmentId department identifier
     * @return employees in the department
     * @throws ResourceNotFoundException if the department does not exist
     */
    @Override
    public List<EmployeeResponse> getByDepartment(Long departmentId) {
        requireDepartmentExists(departmentId);
        return toResponses(employeeRepository.findByDepartmentId(departmentId));
    }

    /**
     * Retrieves active employees.
     *
     * @return all active employees
     */
    @Override
    public List<EmployeeResponse> getActive() {
        return toResponses(employeeRepository.findByActiveTrue());
    }

    /**
     * Marks an employee as inactive.
     *
     * @param id employee identifier
     * @return the deactivated employee
     * @throws ResourceNotFoundException if the employee does not exist
     */
    @Override
    @Transactional
    public EmployeeResponse deactivate(Long id) {
        Employee employee = find(id);
        employee.setActive(false);
        log.info("Deactivated employee id={}", id);
        return EntityMapper.toResponse(employee);
    }

    /**
     * Counts employees in a department.
     *
     * @param departmentId department identifier
     * @return the number of employees in the department
     * @throws ResourceNotFoundException if the department does not exist
     */
    @Override
    public long countByDepartment(Long departmentId) {
        requireDepartmentExists(departmentId);
        return employeeRepository.countByDepartmentId(departmentId);
    }

    /**
     * Retrieves employees hired after a date, ordered by hire date ascending.
     *
     * @param date exclusive hire-date lower bound
     * @return employees hired after the date
     * @throws IllegalArgumentException if the date is null
     */
    @Override
    public List<EmployeeResponse> getHiredAfter(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        return toResponses(employeeRepository.findByHireDateAfterOrderByHireDateAsc(date));
    }

    private Employee find(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }

    private void requireDepartmentExists(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found with id " + departmentId);
        }
    }

    private void apply(Employee employee, EmployeeRequest request, String email) {
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id " + request.departmentId()));
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmail(email);
        employee.setJobTitle(request.jobTitle());
        employee.setSalary(request.salary());
        employee.setHireDate(request.hireDate());
        employee.setActive(request.active() == null || request.active());
        employee.setDepartment(department);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private List<EmployeeResponse> toResponses(List<Employee> employees) {
        return employees.stream().map(EntityMapper::toResponse).toList();
    }
}
