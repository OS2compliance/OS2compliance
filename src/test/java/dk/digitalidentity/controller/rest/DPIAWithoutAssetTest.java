package dk.digitalidentity.controller.rest;

import dk.digitalidentity.BaseIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * En konsekvensanalyse behøver ikke vedrøre et it-system, så både en ny og en tilknyttet
 * konsekvensanalyse kan oprettes uden aktiv.
 */
@Transactional
@AutoConfigureMockMvc
public class DPIAWithoutAssetTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;

    @Test
    public void canCreateDpiaWithoutAsset() throws Exception {
        mockMvc.perform(post("/rest/dpia/create").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Konsekvensanalyse uden aktiv", "assetIds": []}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dpiaId").isNumber());

        assertThat(countDpiasNamed("Konsekvensanalyse uden aktiv")).isEqualTo(1);
    }

    @Test
    public void canCreateExternalDpiaWithoutAsset() throws Exception {
        mockMvc.perform(post("/rest/dpia/external/create").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dpiaId": null, "title": "Tilknyttet konsekvensanalyse uden aktiv", "assetIds": [], "link": "https://example.org/dpia"}
                                """))
                .andExpect(status().isOk());

        assertThat(countDpiasNamed("Tilknyttet konsekvensanalyse uden aktiv")).isEqualTo(1);
    }

    @Test
    public void externalDpiaWithoutTitleGetsADefaultName() throws Exception {
        mockMvc.perform(post("/rest/dpia/external/create").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dpiaId": null, "title": "", "assetIds": [], "link": "https://example.org/dpia"}
                                """))
                .andExpect(status().isOk());

        assertThat(countDpiasNamed("Konsekvensanalyse")).isEqualTo(1);
    }

    private long countDpiasNamed(final String name) {
        return entityManager.createQuery("SELECT COUNT(d) FROM DPIA d WHERE d.name = :name", Long.class)
                .setParameter("name", name)
                .getSingleResult();
    }
}
