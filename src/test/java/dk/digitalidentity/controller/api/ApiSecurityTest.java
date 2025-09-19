package dk.digitalidentity.controller.api;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.model.entity.ApiClient;
import dk.digitalidentity.service.ApiClientService;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.DocumentService;
import dk.digitalidentity.service.OrganisationService;
import dk.digitalidentity.service.SupplierService;
import dk.digitalidentity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
public class ApiSecurityTest extends BaseIntegrationTest {
    @MockitoBean
    private ApiClientService apiClientService;
    @MockitoBean
    private UserService userServiceMock;
    @MockitoBean
    private SupplierService supplierServiceMock;
    @MockitoBean
    private OrganisationService organisationServiceMock;
    @MockitoBean
    private DocumentService documentServiceMock;
    @MockitoBean
    private AssetService assetServiceMock;
    @Autowired
    private MockMvc mockMvc;
    @BeforeEach
    public void setup() {
        doReturn(Page.empty()).when(userServiceMock).getPaged(anyInt(), anyInt());
        doReturn(Page.empty()).when(supplierServiceMock).getPaged(anyInt(), anyInt());
        doReturn(Page.empty()).when(organisationServiceMock).getPaged(anyInt(), anyInt());
        doReturn(Page.empty()).when(documentServiceMock).getPaged(anyInt(), anyInt());
        doReturn(Page.empty()).when(assetServiceMock).getPagedNonDeleted(anyInt(), anyInt());
        doReturn(Optional.empty()).when(apiClientService).getClientByApiKey(anyString());
        doReturn(Optional.of(ApiClient.builder()
            .applicationIdentifier("some-app")
            .id(1L)
            .build()))
            .when(apiClientService).getClientByApiKey("valid-key");
    }

    /**
     * Ensure that we get unauthorized from all controllers when calling with api-key
     */
    @Test
    public void cannotAccessWithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/users").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/suppliers").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/organisations").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/documents").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/assets").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Ensure that we get unauthorized from all controllers when calling with a unknown api-key
     */
    @Test
    public void cannotAccessWithBadApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("ApiKey", "123456").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/suppliers").header("ApiKey", "123456").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/organisations").header("ApiKey", "123456").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/documents").header("ApiKey", "123456").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/assets").header("ApiKey", "123456").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Ensure that we get unauthorized from all controllers when calling with a unknown api-key
     */
    @Test
    public void cannotAccessWithValidKey() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("ApiKey", "valid-key").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/suppliers").header("ApiKey", "valid-key").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/organisations").header("ApiKey", "valid-key").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/documents").header("ApiKey", "valid-key").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/assets").header("ApiKey", "valid-key").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

}
