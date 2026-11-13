package com.ems.integration;

import com.ems.TestData;
import com.ems.model.Department;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional   // each test rolls back, so tests stay independent
class EmployeeApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    private Department engineering;
    private Department hr;

    @BeforeEach
    void setUp() {
        engineering = departmentRepository.save(TestData.department("Engineering"));
        hr = departmentRepository.save(TestData.department("HR"));
    }

    private String json(String first, String last, String email, Long departmentId) {
        return """
                {"firstName":"%s","lastName":"%s","email":"%s","jobTitle":"Engineer",
                 "salary":50000,"hireDate":"2024-01-15","active":true,"departmentId":%d}
                """.formatted(first, last, email, departmentId);
    }

    private Long createViaApi(String first, String last, String email) throws Exception {
        String body = mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                        .content(json(first, last, email, engineering.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    void createEmployee_returns201() throws Exception {
        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Asha", "Rao", "Asha@Example.com", engineering.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fullName").value("Asha Rao"))
                .andExpect(jsonPath("$.email").value("asha@example.com"))
                .andExpect(jsonPath("$.departmentName").value("Engineering"));
    }

    @Test
    void getEmployeeById_returns200() throws Exception {
        Long id = createViaApi("Asha", "Rao", "asha@example.com");

        mockMvc.perform(get("/api/employees/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Asha"))
                .andExpect(jsonPath("$.hireDate").value("2024-01-15"));
    }

    @Test
    void getEmployeeById_missing_returns404() throws Exception {
        mockMvc.perform(get("/api/employees/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found with id 999999"));
    }

    @Test
    void invalidRequest_returns400WithFieldErrors() throws Exception {
        String bad = """
                {"firstName":"","lastName":"Rao","email":"not-an-email","hireDate":"2024-01-15","departmentId":%d}
                """.formatted(engineering.getId());

        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void duplicateEmail_returns409() throws Exception {
        createViaApi("Asha", "Rao", "asha@example.com");

        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Another", "Person", "ASHA@example.com", hr.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownDepartment_returns404() throws Exception {
        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Asha", "Rao", "asha@example.com", 424242L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchByName_returnsMatchingEmployees() throws Exception {
        employeeRepository.save(TestData.employee("Asha", "Rao", "asha@example.com", engineering, true));
        employeeRepository.save(TestData.employee("Ravi", "Kumar", "ravi@example.com", hr, true));

        mockMvc.perform(get("/api/employees/search").param("name", "asha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Asha Rao"));
    }

    @Test
    void filterByDepartment_returnsOnlyThatDepartment() throws Exception {
        employeeRepository.save(TestData.employee("Asha", "Rao", "asha@example.com", engineering, true));
        employeeRepository.save(TestData.employee("Ravi", "Kumar", "ravi@example.com", hr, true));

        mockMvc.perform(get("/api/employees/department/{id}", hr.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].departmentName").value("HR"));

        mockMvc.perform(get("/api/employees/department/{id}", 424242))
                .andExpect(status().isNotFound());
    }

    @Test
    void getActiveEmployees_excludesInactive() throws Exception {
        employeeRepository.save(TestData.employee("Asha", "Rao", "asha@example.com", engineering, true));
        employeeRepository.save(TestData.employee("Ravi", "Kumar", "ravi@example.com", engineering, false));

        mockMvc.perform(get("/api/employees/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void getAllEmployees_returnsList() throws Exception {
        employeeRepository.save(TestData.employee("Asha", "Rao", "asha@example.com", engineering, true));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void updateEmployee_returns200() throws Exception {
        Long id = createViaApi("Asha", "Rao", "asha@example.com");

        mockMvc.perform(put("/api/employees/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(json("Asha", "Rao-Menon", "asha@example.com", hr.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Rao-Menon"))
                .andExpect(jsonPath("$.departmentName").value("HR"));
    }

    @Test
    void deleteEmployee_returns204ThenNotFound() throws Exception {
        Long id = createViaApi("Asha", "Rao", "asha@example.com");

        mockMvc.perform(delete("/api/employees/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/employees/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void deleteDepartmentWithEmployees_isRejectedWith409() throws Exception {
        employeeRepository.save(TestData.employee("Asha", "Rao", "asha@example.com", engineering, true));

        mockMvc.perform(delete("/api/departments/{id}", engineering.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("still has employees")));
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest());
    }
}
