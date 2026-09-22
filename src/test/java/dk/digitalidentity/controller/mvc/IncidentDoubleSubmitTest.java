package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.IncidentDao;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.service.IncidentService;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Formularen har intet id før hændelsen findes, så hver indsendelse oprettede sin egen hændelse.
 * Tokenen ligger nu unikt i basen, så kun den første indsendelse kan oprette noget.
 */
@Transactional
@AutoConfigureMockMvc
public class IncidentDoubleSubmitTest extends BaseIntegrationTest {
    private static final String INCIDENT_NAME = "Hændelse indsendt to gange";

    @Autowired private MockMvc mockMvc;
    @Autowired private IncidentService incidentService;
    @Autowired private IncidentDao incidentDao;

    @Test
    public void secondSubmitOfTheSameFormCreatesNoSecondIncident() throws Exception {
        final String formToken = UUID.randomUUID().toString();

        final String first = submit(formToken, INCIDENT_NAME);
        final String second = submit(formToken, INCIDENT_NAME);

        assertThat(second)
            .as("gensendelsen skal ende på den hændelse den første indsendelse oprettede")
            .isEqualTo(first);
        assertThat(incidentsNamed(INCIDENT_NAME)).isEqualTo(1);
    }

    /** Gik brugeren tilbage og rettede noget, skal rettelsen med - ikke kasseres som en dublet. */
    @Test
    public void secondSubmitWritesItsOwnChangesToThatIncident() throws Exception {
        final String formToken = UUID.randomUUID().toString();

        submit(formToken, INCIDENT_NAME);
        submit(formToken, INCIDENT_NAME + " rettet");

        assertThat(incidentsNamed(INCIDENT_NAME)).isZero();
        assertThat(incidentsNamed(INCIDENT_NAME + " rettet")).isEqualTo(1);
    }

    @Test
    public void aFormOfItsOwnStillCreatesItsOwnIncident() throws Exception {
        submit(UUID.randomUUID().toString(), INCIDENT_NAME);
        submit(UUID.randomUUID().toString(), INCIDENT_NAME);

        assertThat(incidentsNamed(INCIDENT_NAME)).isEqualTo(2);
    }

    /**
     * Det er databasen der afgør kapløbet mellem to samtidige indsendelser - taberen får en
     * constraint-fejl og sendes videre til den hændelse vinderen oprettede.
     */
    @Test
    public void theSameTokenCanOnlyExistOnOneIncident() {
        final String formToken = UUID.randomUUID().toString();
        incidentService.create(newIncident(formToken));

        assertThatThrownBy(() -> incidentService.create(newIncident(formToken)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** Uden feltet i formularen har serveren ikke noget at genkende gensendelsen på. */
    @Test
    public void theCreateFormCarriesAToken() throws Exception {
        final String html = mockMvc.perform(get("/incidents/logForm"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(Jsoup.parse(html).selectFirst("input[name=formToken]")).isNotNull();
    }

    private String submit(final String formToken, final String name) throws Exception {
        return mockMvc.perform(post("/incidents/log").with(csrf().asHeader())
                .param("name", name)
                .param("formToken", formToken))
            .andExpect(status().is3xxRedirection())
            .andReturn().getResponse().getRedirectedUrl();
    }

    private static Incident newIncident(final String formToken) {
        final Incident incident = new Incident();
        incident.setName(INCIDENT_NAME);
        incident.setFormToken(formToken);
        return incident;
    }

    private long incidentsNamed(final String name) {
        return incidentDao.findAll().stream()
            .filter(incident -> name.equals(incident.getName()))
            .count();
    }
}
