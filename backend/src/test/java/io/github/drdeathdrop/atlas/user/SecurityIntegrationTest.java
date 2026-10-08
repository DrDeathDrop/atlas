package io.github.drdeathdrop.atlas.user;

import com.jayway.jsonpath.JsonPath;
import io.github.drdeathdrop.atlas.user.account.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SecurityIntegrationTest {

    private static final String EMAIL_SUFFIX = "@it.atlas.test";
    private static final String PASSWORD = "integration-test-1";
    private static final String REFRESH_COOKIE = "atlas_refresh";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void removeTestUsers() {
        String pattern = "%" + EMAIL_SUFFIX;
        jdbc.update("delete from refresh_tokens where user_id in (select id from users where email like ?)", pattern);
        jdbc.update("delete from users where email like ?", pattern);
    }

    @Test
    void requestWithoutATokenIsRejected() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithAForgedTokenIsRejected() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithAWrongPasswordIsRejected() throws Exception {
        String email = createUser(Role.DISPATCHER);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "not-the-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loggedInUserCanReadTheirOwnDetails() throws Exception {
        String email = createUser(Role.ANALYST);
        String token = accessToken(login(email));

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ANALYST"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void adminCanCreateAUser() throws Exception {
        String token = accessToken(login(createUser(Role.ADMIN)));
        String newEmail = uniqueEmail("created");

        mvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson(newEmail)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.role").value("VIEWER"));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void everyOtherRoleIsForbiddenFromCreatingUsers(Role role) throws Exception {
        String token = accessToken(login(createUser(role)));

        mvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson(uniqueEmail("created"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void creatingAUserWithoutATokenIsRejected() throws Exception {
        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson(uniqueEmail("created"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void creatingAUserWithATakenEmailIsAConflict() throws Exception {
        String token = accessToken(login(createUser(Role.ADMIN)));
        String existing = createUser(Role.VIEWER);

        mvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson(existing)))
                .andExpect(status().isConflict());
    }

    @Test
    void loginPutsTheRefreshTokenInAProtectedCookieAndNotInTheBody() throws Exception {
        MvcResult login = login(createUser(Role.DISPATCHER));

        String setCookie = login.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .startsWith(REFRESH_COOKIE + "=")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("Path=/api/auth");

        String body = login.getResponse().getContentAsString();
        assertThat(body).doesNotContain(refreshCookie(login).getValue());
    }

    @Test
    void refreshIssuesANewAccessTokenAndReplacesTheCookie() throws Exception {
        MvcResult login = login(createUser(Role.DISPATCHER));
        Cookie original = refreshCookie(login);

        MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(original))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(refreshCookie(refreshed).getValue()).isNotEqualTo(original.getValue());

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(refreshed)))
                .andExpect(status().isOk());
    }

    @Test
    void refreshWithoutACookieIsRejected() throws Exception {
        mvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reusingARefreshTokenEndsEverySessionOfThatUser() throws Exception {
        MvcResult login = login(createUser(Role.DISPATCHER));
        Cookie first = refreshCookie(login);

        MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(first))
                .andExpect(status().isOk())
                .andReturn();
        Cookie second = refreshCookie(refreshed);

        mvc.perform(post("/api/auth/refresh").cookie(first))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/refresh").cookie(second))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        MvcResult login = login(createUser(Role.DISPATCHER));
        Cookie cookie = refreshCookie(login);

        mvc.perform(post("/api/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/refresh").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userSeesAndEndsOnlyTheirOwnSessions() throws Exception {
        String ownerEmail = createUser(Role.DISPATCHER);
        login(ownerEmail);
        String ownerToken = accessToken(login(ownerEmail));
        String strangerToken = accessToken(login(createUser(Role.ANALYST)));

        List<String> ownerSessions = sessionIds(ownerToken);
        assertThat(ownerSessions).hasSize(2);
        assertThat(sessionIds(strangerToken)).hasSize(1).doesNotContainAnyElementsOf(ownerSessions);

        mvc.perform(delete("/api/auth/sessions/" + ownerSessions.get(0))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + strangerToken))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/auth/sessions/" + ownerSessions.get(0))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        assertThat(sessionIds(ownerToken)).hasSize(1);
    }

    private String createUser(Role role) {
        String email = uniqueEmail(role.name().toLowerCase());
        userService.createUser(email, PASSWORD, "Test " + role.name(), role);
        return email;
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + EMAIL_SUFFIX;
    }

    private MvcResult login(String email) throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private List<String> sessionIds(String token) throws Exception {
        String body = mvc.perform(get("/api/auth/sessions").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$[*].id");
    }

    private static String accessToken(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static Cookie refreshCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(cookie).isNotNull();
        return new Cookie(REFRESH_COOKIE, cookie.getValue());
    }

    private static String loginJson(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    private static String createUserJson(String email) {
        return "{\"email\":\"%s\",\"password\":\"%s\",\"fullName\":\"Created User\",\"role\":\"VIEWER\"}"
                .formatted(email, PASSWORD);
    }
}
