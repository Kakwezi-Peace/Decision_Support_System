package rw.ac.dss.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end auth checks through the real security filter chain (no @WithMockUser
 * shortcuts) - this is what caught the sendError()/forward-to-/error bug where a
 * role mismatch was silently downgraded from 403 to 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthAndAccessControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("token").asText();
    }

    @Test
    void unauthenticatedRequest_isRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/aircraft"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withCorrectAdminCredentials_returnsToken() throws Exception {
        String token = loginAndGetToken("admin", "ChangeMe123!");
        org.assertj.core.api.Assertions.assertThat(token).isNotBlank();
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"totally-wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"password123\",\"role\":\"OPERATIONS_CONTROLLER\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_asAdmin_succeedsWith201() throws Exception {
        String adminToken = loginAndGetToken("admin", "ChangeMe123!");

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newdispatcher\",\"password\":\"password123\",\"fullName\":\"New Dispatcher\",\"role\":\"OPERATIONS_CONTROLLER\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void register_asDispatcher_isForbiddenWith403_not401() throws Exception {
        String adminToken = loginAndGetToken("admin", "ChangeMe123!");
        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dispatcher-under-test\",\"password\":\"password123\",\"fullName\":\"D\",\"role\":\"OPERATIONS_CONTROLLER\"}"))
                .andExpect(status().isCreated());

        String dispatcherToken = loginAndGetToken("dispatcher-under-test", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"should-not-be-created\",\"password\":\"password123\",\"role\":\"OPERATIONS_CONTROLLER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedDispatcher_canReadOperationalData() throws Exception {
        String adminToken = loginAndGetToken("admin", "ChangeMe123!");
        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"reader-dispatcher\",\"password\":\"password123\",\"fullName\":\"D\",\"role\":\"OPERATIONS_CONTROLLER\"}"))
                .andExpect(status().isCreated());

        String dispatcherToken = loginAndGetToken("reader-dispatcher", "password123");

        mockMvc.perform(get("/api/aircraft")
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk());
    }
}
