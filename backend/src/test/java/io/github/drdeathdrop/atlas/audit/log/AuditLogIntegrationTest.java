package io.github.drdeathdrop.atlas.audit.log;

import com.jayway.jsonpath.JsonPath;
import io.github.drdeathdrop.atlas.user.Role;
import io.github.drdeathdrop.atlas.user.account.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuditLogIntegrationTest {

    private static final String EMAIL_SUFFIX = "@it.atlas.test";
    private static final String PASSWORD = "integration-test-1";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditEntryRepository auditEntries;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void removeTestData() {
        String pattern = "%" + EMAIL_SUFFIX;
        jdbc.update("delete from incidents where reported_by in (select id from users where email like ?)", pattern);
        jdbc.update("delete from refresh_tokens where user_id in (select id from users where email like ?)", pattern);
        jdbc.update("delete from users where email like ?", pattern);
    }

    @Test
    void theApplicationCannotChangeAnAuditRecord() {
        assertThatThrownBy(() -> jdbc.update("update audit_log set new_state = 'TAMPERED'"))
                .isInstanceOf(DataAccessException.class)
                .hasStackTraceContaining("permission denied for table audit_log");
    }

    @Test
    void theApplicationCannotDeleteAnAuditRecord() {
        assertThatThrownBy(() -> jdbc.update("delete from audit_log"))
                .isInstanceOf(DataAccessException.class)
                .hasStackTraceContaining("permission denied for table audit_log");
    }

    @Test
    void reportingAndMovingAnIncidentLeavesATrail() throws Exception {
        String token = loginAs(Role.DISPATCHER);

        String created = mvc.perform(post("/api/incidents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incidentJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String incidentId = JsonPath.read(created, "$.id");

        mvc.perform(patch("/api/incidents/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VERIFIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));

        mvc.perform(get("/api/audit/incidents/" + incidentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("INCIDENT_REPORTED"))
                .andExpect(jsonPath("$[0].newState").value("REPORTED"))
                .andExpect(jsonPath("$[0].actorRole").value("DISPATCHER"))
                .andExpect(jsonPath("$[1].action").value("INCIDENT_STATUS_CHANGED"))
                .andExpect(jsonPath("$[1].previousState").value("REPORTED"))
                .andExpect(jsonPath("$[1].newState").value("VERIFIED"));
    }

    @Test
    void aRefusedMoveLeavesNoTrail() throws Exception {
        String token = loginAs(Role.DISPATCHER);

        String created = mvc.perform(post("/api/incidents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incidentJson()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String incidentId = JsonPath.read(created, "$.id");

        mvc.perform(patch("/api/incidents/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isConflict());

        assertThat(auditEntries.findByEntityTypeAndEntityIdOrderByOccurredAtAsc(
                AuditEntry.INCIDENT, UUID.fromString(incidentId))).hasSize(1);
    }

    private String loginAs(Role role) throws Exception {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + EMAIL_SUFFIX;
        userService.createUser(email, PASSWORD, "Test " + role.name(), role);

        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String incidentJson() {
        return """
                {
                  "title": "Flood in Plovdiv",
                  "description": "The Maritsa has burst its banks near the centre.",
                  "category": "FLOOD",
                  "severity": "CRITICAL",
                  "latitude": 42.1354,
                  "longitude": 24.7453,
                  "affectedPeople": 25000
                }
                """;
    }
}
