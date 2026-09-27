package dk.digitalidentity.controller.rest;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.DPIAService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.List;

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
    @Autowired
    private DPIAService dpiaService;
    @Autowired
    private AssetService assetService;

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

    @Test
    public void reportRendersForDpiaWithoutAsset() throws Exception {
        final DPIA dpia = dpiaService.create(List.of(), "Konsekvensanalyse af manuelle sagsgange", null, null, null);

        final String text = extractText(assetService.getDPIAPdf(dpia));

        // uden aktiv står konsekvensanalysens eget navn i overskriften i stedet for "Konsekvensanalyse vedr. "
        assertThat(text).contains("Konsekvensanalyse af manuelle sagsgange");
    }

    private String extractText(final byte[] pdf) throws Exception {
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            final StringBuilder text = new StringBuilder();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                text.append(PdfTextExtractor.getTextFromPage(document.getPage(page)));
            }
            return text.toString();
        }
    }

    private long countDpiasNamed(final String name) {
        return entityManager.createQuery("SELECT COUNT(d) FROM DPIA d WHERE d.name = :name", Long.class)
                .setParameter("name", name)
                .getSingleResult();
    }
}
