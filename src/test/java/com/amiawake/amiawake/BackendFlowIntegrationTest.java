package com.amiawake.amiawake;

import com.amiawake.amiawake.auth.repository.RefreshTokenRepository;
import com.amiawake.amiawake.friendship.repository.FriendshipRepository;
import com.amiawake.amiawake.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BackendFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        friendshipRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void userRegistrationValidationDuplicateLoginRefreshLogoutAndStatusFlow() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Alice", "password123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.displayName").value("alice"))
                .andExpect(jsonPath("$.timeZone").value("UTC"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("alice", "password123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("ab", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("alice", "wrongpass123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        Tokens initialTokens = login("alice", "password123");
        assertThat(initialTokens.accessToken()).isNotBlank();
        assertThat(initialTokens.refreshToken()).isNotBlank();
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", bearer(initialTokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));

        mockMvc.perform(patch("/api/v1/users/me/status")
                        .header("Authorization", bearer(initialTokens.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TEXT_ONLY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TEXT_ONLY"));

        mockMvc.perform(get("/api/v1/users/me/status")
                        .header("Authorization", bearer(initialTokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TEXT_ONLY"));

        Tokens refreshedTokens = refresh(initialTokens.refreshToken());
        assertThat(refreshedTokens.accessToken()).isNotBlank();
        assertThat(refreshedTokens.refreshToken()).isNotEqualTo(initialTokens.refreshToken());
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(initialTokens.refreshToken())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(refreshedTokens.refreshToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(refreshedTokens.refreshToken())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void friendshipRequestAcceptListDeleteCancelAndRejectFlow() throws Exception {
        Tokens alice = registerAndLogin("alice");
        Tokens bob = registerAndLogin("bob");
        Tokens carol = registerAndLogin("carol");

        mockMvc.perform(post("/api/v1/friendship/requests")
                        .header("Authorization", bearer(alice.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(friendRequestJson("alice")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/api/v1/friendship/requests")
                        .header("Authorization", bearer(alice.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(friendRequestJson("bob")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/friendship/requests")
                        .header("Authorization", bearer(alice.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(friendRequestJson("bob")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(get("/api/v1/friendship/requests/outgoing")
                        .header("Authorization", bearer(alice.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("bob"));

        mockMvc.perform(get("/api/v1/friendship/requests/incoming")
                        .header("Authorization", bearer(bob.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));

        mockMvc.perform(post("/api/v1/friendship/alice/accept")
                        .header("Authorization", bearer(alice.accessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/api/v1/friendship/alice/accept")
                        .header("Authorization", bearer(bob.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/friendship")
                        .header("Authorization", bearer(alice.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("bob"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        mockMvc.perform(get("/api/v1/friendship")
                        .header("Authorization", bearer(bob.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));

        mockMvc.perform(delete("/api/v1/friendship/bob")
                        .header("Authorization", bearer(alice.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/friendship")
                        .header("Authorization", bearer(alice.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(post("/api/v1/friendship/requests")
                        .header("Authorization", bearer(alice.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(friendRequestJson("carol")))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/friendship/requests/alice")
                        .header("Authorization", bearer(carol.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/friendship/requests")
                        .header("Authorization", bearer(alice.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(friendRequestJson("carol")))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/friendship/requests/carol")
                        .header("Authorization", bearer(alice.accessToken())))
                .andExpect(status().isNoContent());

        assertThat(friendshipRepository.count()).isZero();
    }

    private Tokens registerAndLogin(String username) throws Exception {
        register(username);
        return login(username, "password123");
    }

    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(username, "password123")))
                .andExpect(status().isCreated());
    }

    private Tokens login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn();

        return tokensFrom(result);
    }

    private Tokens refresh(String refreshToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn();

        return tokensFrom(result);
    }

    private static Tokens tokensFrom(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return new Tokens(JsonPath.read(json, "$.accessToken"), JsonPath.read(json, "$.refreshToken"));
    }

    private static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private static String userJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private static String loginJson(String username, String password) {
        return userJson(username, password);
    }

    private static String refreshJson(String refreshToken) {
        return "{\"refreshToken\":\"" + refreshToken + "\"}";
    }

    private static String friendRequestJson(String username) {
        return "{\"username\":\"" + username + "\"}";
    }

    private record Tokens(String accessToken, String refreshToken) {
    }
}
