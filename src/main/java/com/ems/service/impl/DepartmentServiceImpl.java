package com.ems.service.impl;

import com.ems.dto.DepartmentRequest;
import com.ems.dto.DepartmentResponse;
import com.ems.exception.BusinessRuleException;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.mapper.EntityMapper;
import com.ems.model.Department;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import com.ems.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Provides department operations backed by the department and employee repositories.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Creates a department.
     *
     * @param request department details
     * @return the created department
     * @throws DuplicateResourceException if a department with the same name exists
     */
    @Override
    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Department '" + name + "' already exists");
        }
        Department department = new Department();
        department.setName(name);
        department.setDescription(request.description());
        Department saved = departmentRepository.save(department);
        log.info("Created department id={} name={}", saved.getId(), saved.getName());
        return EntityMapper.toResponse(saved);
    }

    /**
     * Updates an existing department.
     *
     * @param id department identifier
     * @param request updated department details
     * @return the updated department
     * @throws ResourceNotFoundException if the department does not exist
     * @throws DuplicateResourceException if another department has the requested name
     */
    @Override
    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = find(id);
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Department '" + name + "' already exists");
        }
        department.setName(name);
        department.setDescription(request.description());
        log.info("Updated department id={}", id);
        return EntityMapper.toResponse(department);
    }

    /**
     * Deletes a department that has no employees.
     *
     * @param id department identifier
     * @throws ResourceNotFoundException if the department does not exist
     * @throws BusinessRuleException if the department still has employees
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Department department = find(id);
        if (employeeRepository.existsByDepartmentId(id)) {
            throw new BusinessRuleException("Cannot delete department '" + department.getName()
                    + "' because it still has employees");
        }
        departmentRepository.delete(department);
        log.info("Deleted department id={}", id);
    }

    /**
     * Retrieves a department by identifier.
     *
     * @param id department identifier
     * @return the requested department
     * @throws ResourceNotFoundException if the department does not exist
     */
    @Override
    public DepartmentResponse getById(Long id) {
        return EntityMapper.toResponse(find(id));
    }

    /**
     * Retrieves all departments.
     *
     * @return all departments
     */
    @Override
    public List<DepartmentResponse> getAll() {
        return departmentRepository.findAll().stream().map(EntityMapper::toResponse).toList();
    }

    /**
     * Retrieves a department by name, ignoring case.
     *
     * @param name department name
     * @return the matching department
     * @throws ResourceNotFoundException if no department has the requested name
     */
    @Override
    public DepartmentResponse getByName(String name) {
        String trimmed = name.trim();
        return departmentRepository.findByNameIgnoreCase(trimmed)
                .map(EntityMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with name '" + trimmed + "'"));
    }

    private Department find(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + id));
    }
}
