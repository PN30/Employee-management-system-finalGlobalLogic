package com.ems.service;

import com.ems.TestData;
import com.ems.dto.EmployeeResponse;
import com.ems.dto.ProjectRequest;
import com.ems.dto.ProjectResponse;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.model.Department;
import com.ems.model.Employee;
import com.ems.model.Project;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.ProjectRepository;
import com.ems.service.impl.ProjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @InjectMocks
    private ProjectServiceImpl service;

    private Project project;
    private Employee employee;

    @BeforeEach
    void setUp() {
        Department department = TestData.department("Engineering");
        department.setId(1L);
        project = TestData.project("Apollo");
        project.setId(2L);
        employee = TestData.employee("Asha", "Rao", "asha@example.com", department, true);
        employee.setId(3L);
    }

    private ProjectRequest request(String name) {
        return new ProjectRequest(name, "desc", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 1));
    }

    @Test
    void create_savesProject() {
        when(projectRepository.existsByNameIgnoreCase("Zeus")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(8L);
            return p;
        });

        ProjectResponse response = service.create(request(" Zeus "));

        assertThat(response.id()).isEqualTo(8L);
        assertThat(response.name()).isEqualTo("Zeus");
        assertThat(response.employeeCount()).isZero();
    }

    @Test
    void create_duplicateName_throws() {
        when(projectRepository.existsByNameIgnoreCase("Apollo")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("Apollo"))).isInstanceOf(DuplicateResourceException.class);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void update_changesFields() {
        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));
        when(projectRepository.existsByNameIgnoreCaseAndIdNot("Apollo 2", 2L)).thenReturn(false);

        ProjectResponse response = service.update(2L, request("Apollo 2"));

        assertThat(response.name()).isEqualTo("Apollo 2");
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2024, 6, 1));
    }

    @Test
    void update_duplicateName_throws() {
        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));
        when(projectRepository.existsByNameIgnoreCaseAndIdNot("Other", 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(2L, request("Other"))).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void update_notFound_throws() {
        when(projectRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(2L, request("X"))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_detachesEmployeesThenDeletes() {
        employee.getProjects().add(project);
        project.getEmployees().add(employee);
        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));

        service.delete(2L);

        assertThat(employee.getProjects()).isEmpty();
        assertThat(project.getEmployees()).isEmpty();
        verify(projectRepository).delete(project);
    }

    @Test
    void delete_notFound_throws() {
        when(projectRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(2L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAll_and_getById() {
        when(projectRepository.findAll()).thenReturn(List.of(project));
        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));

        assertThat(service.getAll()).hasSize(1);
        assertThat(service.getById(2L).name()).isEqualTo("Apollo");
    }

    @Test
    void assignEmployee_linksBothSides() {
        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));

        ProjectResponse response = service.assignEmployee(2L, 3L);

        assertThat(response.employeeCount()).isEqualTo(1);
        assertThat(employee.getProjects()).contains(project);
    }

    @Test
    void assignEmployee_alreadyAssigned_throws() {
        employee.getProjects().add(project);
        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.assignEmployee(2L, 3L)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void assignEmployee_unknownProject_throws() {
        when(projectRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignEmployee(2L, 3L))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Project");
    }

    @Test
    void assignEmployee_unknownEmployee_throws() {
        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));
        when(employeeRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignEmployee(2L, 3L))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Employee");
    }

    @Test
    void removeEmployee_unlinksBothSides() {
        employee.getProjects().add(project);
        project.getEmployees().add(employee);
        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));

        ProjectResponse response = service.removeEmployee(2L, 3L);

        assertThat(response.employeeCount()).isZero();
        assertThat(employee.getProjects()).isEmpty();
    }

    @Test
    void removeEmployee_notAssigned_throws() {
        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.removeEmployee(2L, 3L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getEmployeesByProject_returnsTeam() {
        when(projectRepository.existsById(2L)).thenReturn(true);
        when(employeeRepository.findByProjects_Id(2L)).thenReturn(List.of(employee));

        assertThat(service.getEmployeesByProject(2L)).extracting(EmployeeResponse::fullName)
                .containsExactly("Asha Rao");
    }

    @Test
    void getEmployeesByProject_unknownProject_throws() {
        when(projectRepository.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.getEmployeesByProject(2L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProjectsByEmployee_returnsProjects() {
        when(employeeRepository.existsById(3L)).thenReturn(true);
        when(projectRepository.findByEmployees_Id(3L)).thenReturn(List.of(project));

        assertThat(service.getProjectsByEmployee(3L)).extracting(ProjectResponse::name).containsExactly("Apollo");
    }

    @Test
    void getProjectsByEmployee_unknownEmployee_throws() {
        when(employeeRepository.existsById(3L)).thenReturn(false);

        assertThatThrownBy(() -> service.getProjectsByEmployee(3L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
