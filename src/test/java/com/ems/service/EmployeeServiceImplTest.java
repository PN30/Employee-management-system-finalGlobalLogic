package com.ems.service;

import com.ems.TestData;
import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.model.Department;
import com.ems.model.Employee;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import com.ems.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @InjectMocks
    private EmployeeServiceImpl service;

    private Department department;
    private Employee existing;

    @BeforeEach
    void setUp() {
        department = TestData.department("Engineering");
        department.setId(1L);
        existing = TestData.employee("Asha", "Rao", "asha@example.com", department, true);
        existing.setId(5L);
    }

    private EmployeeRequest request(String email, Boolean active) {
        return new EmployeeRequest(" Asha ", "Rao", email, "Engineer", new BigDecimal("60000"),
                LocalDate.of(2024, 1, 15), active, 1L);
    }

    @Test
    void create_normalisesEmailAndSaves() {
        when(employeeRepository.existsByEmailIgnoreCase("asha@example.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        EmployeeResponse response = service.create(request("  Asha@Example.com ", null));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.email()).isEqualTo("asha@example.com");
        assertThat(response.firstName()).isEqualTo("Asha");
        assertThat(response.fullName()).isEqualTo("Asha Rao");
        assertThat(response.active()).isTrue();
        assertThat(response.departmentName()).isEqualTo("Engineering");
    }

    @Test
    void create_respectsExplicitInactiveFlag() {
        when(employeeRepository.existsByEmailIgnoreCase("asha@example.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.create(request("asha@example.com", false)).active()).isFalse();
    }

    @Test
    void create_duplicateEmail_throws() {
        when(employeeRepository.existsByEmailIgnoreCase("asha@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("asha@example.com", true)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void create_unknownDepartment_throws() {
        when(employeeRepository.existsByEmailIgnoreCase("asha@example.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request("asha@example.com", true)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department");
    }

    @Test
    void update_changesFields() {
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(employeeRepository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 5L)).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        EmployeeResponse response = service.update(5L, request("new@example.com", true));

        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.salary()).isEqualByComparingTo("60000");
    }

    @Test
    void update_notFound_throws() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request("a@b.com", true)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_emailTakenByAnotherEmployee_throws() {
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(employeeRepository.existsByEmailIgnoreCaseAndIdNot("taken@example.com", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, request("taken@example.com", true)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void delete_removesEmployee() {
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(existing));

        service.delete(5L);

        verify(employeeRepository).delete(existing);
    }

    @Test
    void delete_notFound_throws() {
        when(employeeRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(ResourceNotFoundException.class);
        verify(employeeRepository, never()).delete(any());
    }

    @Test
    void getById_found() {
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThat(service.getById(5L).email()).isEqualTo("asha@example.com");
    }

    @Test
    void getById_missing_throws() {
        when(employeeRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAll_returnsEveryone() {
        when(employeeRepository.findAll()).thenReturn(List.of(existing));

        assertThat(service.getAll()).hasSize(1);
    }

    @Test
    void searchByName_blankReturnsAll() {
        when(employeeRepository.findAll()).thenReturn(List.of(existing));

        assertThat(service.searchByName("   ")).hasSize(1);
        assertThat(service.searchByName(null)).hasSize(1);
        verify(employeeRepository, never()).searchByName(any());
    }

    @Test
    void searchByName_trimsAndDelegates() {
        when(employeeRepository.searchByName("asha")).thenReturn(List.of(existing));

        assertThat(service.searchByName(" asha ")).extracting(EmployeeResponse::fullName).containsExactly("Asha Rao");
    }

    @Test
    void getByDepartment_returnsMembers() {
        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(employeeRepository.findByDepartmentId(1L)).thenReturn(List.of(existing));

        assertThat(service.getByDepartment(1L)).hasSize(1);
    }

    @Test
    void getByDepartment_unknownDepartment_throws() {
        when(departmentRepository.existsById(3L)).thenReturn(false);

        assertThatThrownBy(() -> service.getByDepartment(3L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getActive_returnsOnlyActive() {
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(existing));

        assertThat(service.getActive()).allMatch(EmployeeResponse::active);
    }
}
