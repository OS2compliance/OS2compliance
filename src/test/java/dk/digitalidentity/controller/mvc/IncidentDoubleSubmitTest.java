package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.IncidentDao;
import dk.digitalidentity.model.entity.Incident;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Et gem opretter en hændelse hver gang formularen sendes, og formularen har intet id før hændelsen
 * findes. Klikkes Gem igen mens det første gem er undervejs, bliver den samme hændelse oprettet en
 * gang til. Engangstokenen gør den anden indsendelse til et opslag i stedet.
 */
@Transactional
@AutoConfigureMockMvc
public class IncidentDoubleSubmitTest extends BaseIntegrationTest {
    private static final String INCIDENT_NAME = "Hændelse indsendt to gange";

    @Autowired private MockMvc mockMvc;
    @Autowired private IncidentController incidentController;
    @Autowired private IncidentDao incidentDao;

    @Test
    public void secondSubmitOfTheSameFormLandsOnTheIncidentTheFirstOneCreated() {
        final MockHttpSession session = new MockHttpSession();
        final String formToken = UUID.randomUUID().toString();

        final String first = incidentController.createOrUpdateIncident(newIncident(), formToken, session);
        final String second = incidentController.createOrUpdateIncident(newIncident(), formToken, session);

        assertThat(second)
            .as("gensendelsen skal vise den hændelse der allerede blev oprettet")
            .isEqualTo(first);
        assertThat(createdIncidents()).isEqualTo(1);
    }

    @Test
    public void aFormOfItsOwnStillCreatesItsOwnIncident() {
        final MockHttpSession session = new MockHttpSession();

        incidentController.createOrUpdateIncident(newIncident(), UUID.randomUUID().toString(), session);
        incidentController.createOrUpdateIncident(newIncident(), UUID.randomUUID().toString(), session);

        assertThat(createdIncidents()).isEqualTo(2);
    }

    /** Uden feltet i formularen har serveren ikke noget at genkende gensendelsen på. */
    @Test
    public void theCreateFormCarriesAToken() throws Exception {
        final String html = mockMvc.perform(get("/incidents/logForm"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(Jsoup.parse(html).selectFirst("input[name=formToken]")).isNotNull();
    }

    private static Incident newIncident() {
        final Incident incident = new Incident();
        incident.setName(INCIDENT_NAME);
        return incident;
    }

    private long createdIncidents() {
        return incidentDao.findAll().stream()
            .filter(incident -> INCIDENT_NAME.equals(incident.getName()))
            .count();
    }
}
