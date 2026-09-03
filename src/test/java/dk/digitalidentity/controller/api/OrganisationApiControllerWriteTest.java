package dk.digitalidentity.controller.api;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.OrganisationUnitDao;
import dk.digitalidentity.model.entity.ApiClient;
import dk.digitalidentity.model.entity.OrganisationUnit;
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
 * Integration tests for the write operations of {@link OrganisationApiController}
 */
@Transactional
@AutoConfigureMockMvc
public class OrganisationApiControllerWriteTest extends BaseIntegrationTest {
    private static final String PARENT_UUID = "30000000-0000-0000-0000-000000000001";
    private static final String CHILD_UUID = "30000000-0000-0000-0000-000000000002";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ApiClientService apiClientService;
    @Autowired
    private OrganisationUnitDao organisationUnitDao;
    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    public void setup() {
        doReturn(Optional.of(new ApiClient())).when(apiClientService).getClientByApiKey(anyString());
        organisationUnitDao.save(OrganisationUnit.builder().uuid(PARENT_UUID).name("Forvaltningen").active(true).build());
    }

    @Test
    public void canCreateOrganisationUnit() throws Exception {
        mockMvc.perform(post("/api/v1/organisations")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "uuid": "%s", "name": "Borgerservice", "parentUuid": "%s" }
                    """.formatted(CHILD_UUID, PARENT_UUID)))
            .andDo(print()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.uuid").value(CHILD_UUID))
            .andExpect(jsonPath("$.name").value("Borgerservice"))
            .andExpect(jsonPath("$.parentUuid").value(PARENT_UUID))
            .andExpect(jsonPath("$.active").value(true));

        flushAndClear();
        final OrganisationUnit created = organisationUnitDao.findById(CHILD_UUID).orElseThrow();
        assertEquals("Borgerservice", created.getName());
        assertEquals(PARENT_UUID, created.getParentUuid());
    }

    @Test
    public void canCreateOrganisationUnitWithoutUuid() throws Exception {
        mockMvc.perform(post("/api/v1/organisations")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Borgerservice" }
                    """))
            .andDo(print()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.uuid").isNotEmpty());
    }

    @Test
    public void createOrganisationUnitWithEmptyUuidGeneratesUuid() throws Exception {
        mockMvc.perform(post("/api/v1/organisations")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "uuid": "", "name": "Borgerservice" }
                    """))
            .andDo(print()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.uuid").isNotEmpty());

        flushAndClear();
        assertTrue(organisationUnitDao.findById("").isEmpty(), "an empty uuid must never become a primary key");
    }

    @Test
    public void createOrganisationUnitRequiresName() throws Exception {
        mockMvc.perform(post("/api/v1/organisations")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andDo(print()).andExpect(status().isBadRequest());
    }

    @Test
    public void createOrganisationUnitWithUnknownParentFails() throws Exception {
        mockMvc.perform(post("/api/v1/organisations")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Borgerservice", "parentUuid": "99999999-0000-0000-0000-000000000000" }
                    """))
            .andDo(print()).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Parent organisation unit not found: 99999999-0000-0000-0000-000000000000"));
    }

    @Test
    public void createOrganisationUnitWithExistingUuidConflicts() throws Exception {
        mockMvc.perform(post("/api/v1/organisations")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "uuid": "%s", "name": "Borgerservice" }
                    """.formatted(PARENT_UUID)))
            .andDo(print()).andExpect(status().isConflict());
    }

    @Test
    public void canUpdateOrganisationUnit() throws Exception {
        organisationUnitDao.save(OrganisationUnit.builder().uuid(CHILD_UUID).name("Borgerservice").active(true).build());

        mockMvc.perform(put("/api/v1/organisations/{uuid}", CHILD_UUID)
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Borgerservice Øst", "parentUuid": "%s", "active": false }
                    """.formatted(PARENT_UUID)))
            .andDo(print()).andExpect(status().isNoContent());

        flushAndClear();
        final OrganisationUnit updated = organisationUnitDao.findById(CHILD_UUID).orElseThrow();
        assertEquals("Borgerservice Øst", updated.getName());
        assertEquals(PARENT_UUID, updated.getParentUuid());
        assertEquals(false, updated.getActive());
    }

    @Test
    public void updateRejectsCyclicParent() throws Exception {
        organisationUnitDao.save(OrganisationUnit.builder().uuid(CHILD_UUID).name("Borgerservice").parentUuid(PARENT_UUID).active(true).build());

        mockMvc.perform(put("/api/v1/organisations/{uuid}", PARENT_UUID)
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Forvaltningen", "parentUuid": "%s", "active": true }
                    """.formatted(CHILD_UUID)))
            .andDo(print()).andExpect(status().isBadRequest());
    }

    @Test
    public void updateRejectsSelfAsParent() throws Exception {
        mockMvc.perform(put("/api/v1/organisations/{uuid}", PARENT_UUID)
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Forvaltningen", "parentUuid": "%s", "active": true }
                    """.formatted(PARENT_UUID)))
            .andDo(print()).andExpect(status().isBadRequest());
    }

    @Test
    public void updateUnknownOrganisationUnitReturnsNotFound() throws Exception {
        mockMvc.perform(put("/api/v1/organisations/{uuid}", "99999999-0000-0000-0000-000000000000")
                .header("ApiKey", "dummy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Findes Ikke", "active": true }
                    """))
            .andDo(print()).andExpect(status().isNotFound());
    }

    @Test
    public void writeOperationsRequireApiKey() throws Exception {
        doReturn(Optional.empty()).when(apiClientService).getClientByApiKey(anyString());
        mockMvc.perform(post("/api/v1/organisations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Borgerservice" }
                    """))
            .andDo(print()).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/organisations/{uuid}", PARENT_UUID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Forvaltningen", "active": true }
                    """))
            .andDo(print()).andExpect(status().isUnauthorized());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

}
