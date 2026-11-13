package com.ems.repository;

import com.ems.TestData;
import com.ems.model.Department;
import com.ems.model.Employee;
import com.ems.model.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Department engineering;
    private Department hr;

    @BeforeEach
    void setUp() {
        engineering = departmentRepository.save(TestData.department("Engineering"));
        hr = departmentRepository.save(TestData.department("HR"));
        employeeRepository.save(TestData.employee("Asha", "Rao", "asha@example.com", engineering, true));
        employeeRepository.save(TestData.employee("Ravi", "Kumar", "ravi@example.com", engineering, false));
        employeeRepository.save(TestData.employee("Meera", "Iyer", "meera@example.com", hr, true));
    }

    @Test
    void findByDepartmentId_returnsOnlyThatDepartment() {
        List<Employee> result = employeeRepository.findByDepartmentId(engineering.getId());

        assertThat(result).extracting(Employee::getFirstName).containsExactlyInAnyOrder("Asha", "Ravi");
    }

    @Test
    void findByActiveTrue_excludesInactive() {
        assertThat(employeeRepository.findByActiveTrue())
                .extracting(Employee::getFirstName).containsExactlyInAnyOrder("Asha", "Meera");
    }

    @Test
    void searchByName_isCaseInsensitiveAndMatchesFullName() {
        assertThat(employeeRepository.searchByName("ASHA")).hasSize(1);
        assertThat(employeeRepository.searchByName("a r")).extracting(Employee::getLastName).contains("Rao");
        assertThat(employeeRepository.searchByName("zzz")).isEmpty();
    }

    @Test
    void existsByEmail_variants() {
        Employee asha = employeeRepository.findByActiveTrue().stream()
                .filter(e -> e.getFirstName().equals("Asha")).findFirst().orElseThrow();

        assertThat(employeeRepository.existsByEmailIgnoreCase("ASHA@example.com")).isTrue();
        assertThat(employeeRepository.existsByEmailIgnoreCaseAndIdNot("asha@example.com", asha.getId())).isFalse();
        assertThat(employeeRepository.existsByEmailIgnoreCaseAndIdNot("asha@example.com", -1L)).isTrue();
        assertThat(employeeRepository.existsByDepartmentId(hr.getId())).isTrue();
    }

    @Test
    void duplicateEmail_violatesUniqueConstraint() {
        Employee duplicate = TestData.employee("Other", "Person", "asha@example.com", hr, true);

        assertThatThrownBy(() -> employeeRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingDepartmentWithEmployees_violatesForeignKey() {
        employeeRepository.flush();

        assertThatThrownBy(() -> jdbcTemplate.update("delete from department where id = ?", engineering.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void manyToMany_queriesWorkFromBothSides() {
        Project apollo = projectRepository.save(TestData.project("Apollo"));
        Employee asha = employeeRepository.findByActiveTrue().stream()
                .filter(e -> e.getFirstName().equals("Asha")).findFirst().orElseThrow();
        asha.getProjects().add(apollo);
        employeeRepository.flush();

        assertThat(employeeRepository.findByProjects_Id(apollo.getId())).extracting(Employee::getEmail)
                .containsExactly("asha@example.com");
        assertThat(projectRepository.findByEmployees_Id(asha.getId())).extracting(Project::getName)
                .containsExactly("Apollo");
    }

    @Test
    void projectAndDepartmentNameExistenceChecks() {
        projectRepository.save(TestData.project("Apollo"));

        assertThat(projectRepository.existsByNameIgnoreCase("apollo")).isTrue();
        assertThat(projectRepository.existsByNameIgnoreCaseAndIdNot("apollo", -1L)).isTrue();
        assertThat(departmentRepository.existsByNameIgnoreCase("hr")).isTrue();
        assertThat(departmentRepository.existsByNameIgnoreCaseAndIdNot("hr", hr.getId())).isFalse();
    }
}
