package dk.digitalidentity.controller.api;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.OrganisationUnitDao;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.ApiClient;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Position;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.service.ApiClientService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the write operations of {@link UserApiController}
 */
@Transactional
@AutoConfigureMockMvc
public class UserApiControllerWriteTest extends BaseIntegrationTest {
    private static final String OU_UUID = "10000000-0000-0000-0000-000000000001";
    private static final String OTHER_OU_UUID = "10000000-0000-0000-0000-000000000002";
    private static final String INACTIVE_OU_UUID = "10000000-0000-0000-0000-000000000003";
    private static final String USER_UUID = "20000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ApiClientService apiClientService;
    @Autowired
    private UserDao userDao;
    @Autowired
    private OrganisationUnitDao organisationUnitDao;
    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    public void setup() {
        doReturn(Optional.of(new ApiClient())).when(apiClientService).getClientByApiKey(anyString());
        organisationUnitDao.save(OrganisationUnit.builder().uuid(OU_UUID).name("Enhed 1").active(true).build());
        organisationUnitDao.save(OrganisationUnit.builder().uuid(OTHER_OU_UUID).name("Enhed 2").active(true).build());
        organisationUnitDao.save(OrganisationUnit.builder().uuid(INACTIVE_OU_UUID).name("Nedlagt enhed").active(false).build());
    }

    @Test
    public void canCreateUserWithPositions() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "uuid": "%s",
                        "userId": "abc",
                        "name": "Anders Bording Carlsen",
                        "email": "abc@kommune.dk",
                        "positions": [ { "ouUuid": "%s", "name": "Sagsbehandler" } ]
                    }
                    """.formatted(USER_UUID, OU_UUID)))
            .andDo(print()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.uuid").value(USER_UUID))
            .andExpect(jsonPath("$.userId").value("abc"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.positions[0].ouUuid").value(OU_UUID));

        flushAndClear();
        final User created = userDao.findById(USER_UUID).orElseThrow();
        assertEquals("Anders Bording Carlsen", created.getName());
        assertTrue(created.getActive());
        assertEquals(Set.of(OU_UUID), positionOuUuids(created));
        assertEquals(1, countPositions(USER_UUID));
    }

    @Test
    public void canCreateUserWithoutUuid() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "userId": "abc", "name": "Anders Bording Carlsen" }
                    """))
            .andDo(print()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.uuid").isNotEmpty());
    }

    @Test
    public void createUserWithEmptyUuidGeneratesUuid() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "uuid": "", "userId": "abc", "name": "Anders Bording Carlsen" }
                    """))
            .andDo(print()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.uuid").isNotEmpty());

        flushAndClear();
        assertTrue(userDao.findById("").isEmpty(), "an empty uuid must never become a primary key");
    }

    @Test
    public void createUserRequiresUserIdAndName() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Uden Bruger Id" }
                    """))
            .andDo(print()).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    public void createUserWithTooLongNameFails() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "userId": "abc", "name": "%s" }
                    """.formatted("x".repeat(300))))
            .andDo(print()).andExpect(status().isBadRequest());
    }

    @Test
    public void createUserWithUnknownOuFails() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "userId": "abc",
                        "name": "Anders Bording Carlsen",
                        "positions": [ { "ouUuid": "99999999-0000-0000-0000-000000000000" } ]
                    }
                    """))
            .andDo(print()).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Active organisation unit not found: 99999999-0000-0000-0000-000000000000"));
    }

    @Test
    public void createUserWithInactiveOuFails() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "userId": "abc",
                        "name": "Anders Bording Carlsen",
                        "positions": [ { "ouUuid": "%s" } ]
                    }
                    """.formatted(INACTIVE_OU_UUID)))
            .andDo(print()).andExpect(status().isBadRequest());
    }

    @Test
    public void createUserWithBlankOuUuidFails() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "userId": "abc",
                        "name": "Anders Bording Carlsen",
                        "positions": [ { "ouUuid": "" } ]
                    }
                    """))
            .andDo(print()).andExpect(status().isBadRequest());
    }

    @Test
    public void createUserWithExistingUuidConflicts() throws Exception {
        userDao.save(User.builder().uuid(USER_UUID).userId("existing").name("Eksisterende Bruger").active(true).build());
        mockMvc.perform(post("/api/v1/users")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "uuid": "%s", "userId": "abc", "name": "Anders Bording Carlsen" }
                    """.formatted(USER_UUID)))
            .andDo(print()).andExpect(status().isConflict());
    }

    @Test
    public void createUserWithExistingUserIdConflicts() throws Exception {
        userDao.save(User.builder().uuid(USER_UUID).userId("abc").name("Eksisterende Bruger").active(true).build());
        mockMvc.perform(post("/api/v1/users")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "userId": "abc", "name": "Anders Bording Carlsen" }
                    """))
            .andDo(print()).andExpect(status().isConflict());
    }

    @Test
    public void createUserWithInactiveUsersUserIdConflicts() throws Exception {
        // an inactive twin would be reactivated by the nightly sync, leaving two active users with the same userId
        userDao.save(User.builder().uuid(USER_UUID).userId("abc").name("Inaktiv Bruger").active(false).build());
        mockMvc.perform(post("/api/v1/users")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "userId": "abc", "name": "Anders Bording Carlsen" }
                    """))
            .andDo(print()).andExpect(status().isConflict());
    }

    @Test
    public void canUpdateUserAndReplacePositions() throws Exception {
        final User user = User.builder().uuid(USER_UUID).userId("abc").name("Anders Bording Carlsen").active(true).build();
        user.getPositions().add(Position.builder().user(user).ouUuid(OU_UUID).name("Sagsbehandler").build());
        userDao.save(user);
        flushAndClear();

        mockMvc.perform(put("/api/v1/users/{uuid}", USER_UUID)
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "userId": "abc2",
                        "name": "Anders Bording Carlsen-Dam",
                        "email": "abcd@kommune.dk",
                        "active": false,
                        "positions": [ { "ouUuid": "%s", "name": "Teamleder" } ]
                    }
                    """.formatted(OTHER_OU_UUID)))
            .andDo(print()).andExpect(status().isNoContent());

        flushAndClear();
        final User updated = userDao.findById(USER_UUID).orElseThrow();
        assertEquals("abc2", updated.getUserId());
        assertEquals("Anders Bording Carlsen-Dam", updated.getName());
        assertEquals("abcd@kommune.dk", updated.getEmail());
        assertEquals(false, updated.getActive());
        assertEquals(Set.of(OTHER_OU_UUID), positionOuUuids(updated));
        assertEquals(1, countPositions(USER_UUID));
    }

    @Test
    public void updateUserWithoutPositionsRemovesAll() throws Exception {
        final User user = User.builder().uuid(USER_UUID).userId("abc").name("Anders Bording Carlsen").active(true).build();
        user.getPositions().add(Position.builder().user(user).ouUuid(OU_UUID).name("Sagsbehandler").build());
        userDao.save(user);
        flushAndClear();

        mockMvc.perform(put("/api/v1/users/{uuid}", USER_UUID)
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "userId": "abc", "name": "Anders Bording Carlsen", "active": true }
                    """))
            .andDo(print()).andExpect(status().isNoContent());

        flushAndClear();
        assertEquals(0, countPositions(USER_UUID));
    }

    @Test
    public void updateUserWithTakenUserIdConflicts() throws Exception {
        userDao.save(User.builder().uuid(USER_UUID).userId("abc").name("Anders Bording Carlsen").active(true).build());
        userDao.save(User.builder().uuid("20000000-0000-0000-0000-000000000002").userId("def").name("Dorthe Egelund Frandsen").active(true).build());

        mockMvc.perform(put("/api/v1/users/{uuid}", USER_UUID)
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "userId": "def", "name": "Anders Bording Carlsen", "active": true }
                    """))
            .andDo(print()).andExpect(status().isConflict());
    }

    @Test
    public void updateUnknownUserReturnsNotFound() throws Exception {
        mockMvc.perform(put("/api/v1/users/{uuid}", "99999999-0000-0000-0000-000000000000")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "userId": "abc", "name": "Anders Bording Carlsen", "active": true }
                    """))
            .andDo(print()).andExpect(status().isNotFound());
    }

    @Test
    public void writeOperationsRequireApiKey() throws Exception {
        doReturn(Optional.empty()).when(apiClientService).getClientByApiKey(anyString());
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "userId": "abc", "name": "Anders Bording Carlsen" }
                    """))
            .andDo(print()).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/users/{uuid}", USER_UUID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "userId": "abc", "name": "Anders Bording Carlsen", "active": true }
                    """))
            .andDo(print()).andExpect(status().isUnauthorized());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private long countPositions(final String userUuid) {
        return entityManager.createQuery("select count(p) from Position p where p.user.uuid = :uuid", Long.class)
            .setParameter("uuid", userUuid)
            .getSingleResult();
    }

    private static Set<String> positionOuUuids(final User user) {
        return user.getPositions().stream().map(Position::getOuUuid).collect(Collectors.toSet());
    }

}
