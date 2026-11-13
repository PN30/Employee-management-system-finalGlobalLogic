package com.ems.integration;

import com.ems.TestData;
import com.ems.model.Department;
import com.ems.model.Employee;
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
@Transactional
class DepartmentAndProjectApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    private Employee asha;

    @BeforeEach
    void setUp() {
        Department engineering = departmentRepository.save(TestData.department("Engineering"));
        asha = employeeRepository.save(TestData.employee("Asha", "Rao", "asha@example.com", engineering, true));
    }

    private Long createProject(String name) throws Exception {
        String body = """
                {"name":"%s","description":"demo","startDate":"2024-01-01","endDate":"2024-12-31"}
                """.formatted(name);
        String response = mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    // ---------- departments ----------

    @Test
    void departmentCrudFlow() throws Exception {
        String created = mockMvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Finance\",\"description\":\"Money\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Finance"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(put("/api/departments/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Finance & Ops\",\"description\":\"Money\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Finance & Ops"));

        mockMvc.perform(get("/api/departments/{id}", id)).andExpect(status().isOk());
        mockMvc.perform(get("/api/departments")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(delete("/api/departments/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/departments/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void duplicateDepartmentName_returns409() throws Exception {
        mockMvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"engineering\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void blankDepartmentName_returns400() throws Exception {
        mockMvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    // ---------- projects ----------

    @Test
    void projectCrudFlow() throws Exception {
        Long id = createProject("Apollo");

        mockMvc.perform(put("/api/projects/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Apollo 2\",\"description\":\"v2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Apollo 2"));

        mockMvc.perform(get("/api/projects/{id}", id)).andExpect(status().isOk());
        mockMvc.perform(get("/api/projects")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(delete("/api/projects/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/projects/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void projectWithEndBeforeStart_returns400() throws Exception {
        mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad\",\"startDate\":\"2024-05-01\",\"endDate\":\"2024-01-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateProjectName_returns409() throws Exception {
        createProject("Apollo");

        mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"APOLLO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void assignAndRemoveEmployee_fullFlow() throws Exception {
        Long projectId = createProject("Apollo");

        mockMvc.perform(post("/api/projects/{p}/employees/{e}", projectId, asha.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount").value(1));

        mockMvc.perform(post("/api/projects/{p}/employees/{e}", projectId, asha.getId()))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/projects/{p}/employees", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Asha Rao"));

        mockMvc.perform(get("/api/employees/{e}/projects", asha.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Apollo"));

        mockMvc.perform(delete("/api/projects/{p}/employees/{e}", projectId, asha.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount").value(0));

        mockMvc.perform(delete("/api/projects/{p}/employees/{e}", projectId, asha.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingProjectWithTeam_detachesEmployees() throws Exception {
        Long projectId = createProject("Apollo");
        mockMvc.perform(post("/api/projects/{p}/employees/{e}", projectId, asha.getId())).andExpect(status().isOk());

        mockMvc.perform(delete("/api/projects/{p}", projectId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/employees/{e}/projects", asha.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unknownProjectOrEmployee_returns404() throws Exception {
        mockMvc.perform(get("/api/projects/{p}/employees", 424242)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/employees/{e}/projects", 424242)).andExpect(status().isNotFound());
    }
}
