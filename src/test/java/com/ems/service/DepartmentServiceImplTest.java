package com.ems.service;

import com.ems.TestData;
import com.ems.dto.DepartmentRequest;
import com.ems.dto.DepartmentResponse;
import com.ems.exception.BusinessRuleException;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.model.Department;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import com.ems.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @InjectMocks
    private DepartmentServiceImpl service;

    private Department existing;

    @BeforeEach
    void setUp() {
        existing = TestData.department("Engineering");
        existing.setId(1L);
    }

    @Test
    void create_savesAndReturnsDepartment() {
        when(departmentRepository.existsByNameIgnoreCase("Finance")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
            Department d = inv.getArgument(0);
            d.setId(7L);
            return d;
        });

        DepartmentResponse response = service.create(new DepartmentRequest("  Finance ", "Money"));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.name()).isEqualTo("Finance");
        assertThat(response.employeeCount()).isZero();
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(departmentRepository.existsByNameIgnoreCase("Engineering")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new DepartmentRequest("Engineering", null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Engineering");
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void update_changesNameAndDescription() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("Platform", 1L)).thenReturn(false);

        DepartmentResponse response = service.update(1L, new DepartmentRequest("Platform", "Infra"));

        assertThat(response.name()).isEqualTo("Platform");
        assertThat(response.description()).isEqualTo("Infra");
    }

    @Test
    void update_notFound_throws() {
        when(departmentRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(9L, new DepartmentRequest("X", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_duplicateName_throws() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("HR", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new DepartmentRequest("HR", null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void delete_emptyDepartment_deletes() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.existsByDepartmentId(1L)).thenReturn(false);

        service.delete(1L);

        verify(departmentRepository).delete(existing);
    }

    @Test
    void delete_departmentWithEmployees_isRejected() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.existsByDepartmentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BusinessRuleException.class);
        verify(departmentRepository, never()).delete(any());
    }

    @Test
    void delete_notFound_throws() {
        when(departmentRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(2L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAll_returnsMappedList() {
        when(departmentRepository.findAll()).thenReturn(List.of(existing));

        assertThat(service.getAll()).extracting(DepartmentResponse::name).containsExactly("Engineering");
    }

    @Test
    void getById_returnsDepartment() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThat(service.getById(1L).name()).isEqualTo("Engineering");
    }
}
