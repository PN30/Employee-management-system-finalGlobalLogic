package com.ems.repository;

import com.ems.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByDepartmentId(Long departmentId);

    List<Employee> findByDepartmentId(Long departmentId);

    long countByDepartmentId(Long departmentId);

    List<Employee> findByHireDateAfterOrderByHireDateAsc(LocalDate date);

    List<Employee> findByActiveTrue();

    List<Employee> findByProjects_Id(Long projectId);

    /** JPQL: case-insensitive match on "first last". */
    @Query("""
            select e from Employee e
            where lower(concat(e.firstName, ' ', e.lastName)) like lower(concat('%', :name, '%'))
            order by e.firstName, e.lastName
            """)
    List<Employee> searchByName(@Param("name") String name);
}
