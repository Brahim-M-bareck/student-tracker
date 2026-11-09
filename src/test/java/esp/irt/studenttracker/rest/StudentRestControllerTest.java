package esp.irt.studenttracker.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration des endpoints REST avec MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StudentRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void helloShouldReturnHelloWorld() throws Exception {
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello World!"));
    }

    @Test
    void studentsShouldReturnTheThreeStudents() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].firstName").value("Poornima"))
                .andExpect(jsonPath("$[0].lastName").value("Patel"))
                .andExpect(jsonPath("$[1].firstName").value("Mario"))
                .andExpect(jsonPath("$[1].lastName").value("Rossi"))
                .andExpect(jsonPath("$[2].firstName").value("Mary"))
                .andExpect(jsonPath("$[2].lastName").value("Smith"));
    }

    @Test
    void student2ShouldReturnMarySmith() throws Exception {
        mockMvc.perform(get("/api/students/2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName").value("Mary"))
                .andExpect(jsonPath("$.lastName").value("Smith"));
    }

    @Test
    void unknownStudentShouldReturn404WithJson() throws Exception {
        mockMvc.perform(get("/api/students/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student id not found - 999"))
                .andExpect(jsonPath("$.timeStamp").isNumber());
    }

    @Test
    void nonNumericStudentIdShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/students/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isString());
    }
}
