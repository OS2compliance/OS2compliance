package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.IncidentDao;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.service.IncidentService;
import jakarta.servlet.http.Cookie;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Formularen har intet id før hændelsen findes, så hver indsendelse oprettede sin egen hændelse.
 * Engangstokenen gør den anden indsendelse til en redigering af den første.
 */
@Transactional
@AutoConfigureMockMvc
public class IncidentDoubleSubmitTest extends BaseIntegrationTest {
    private static final String INCIDENT_NAME = "Hændelse indsendt to gange";

    @Autowired private MockMvc mockMvc;
    @Autowired private IncidentController incidentController;
    @Autowired private IncidentService incidentService;
    @Autowired private IncidentDao incidentDao;

    private Cookie[] cookies = new Cookie[0];

    @Test
    public void secondSubmitOfTheSameFormCreatesNoSecondIncident() throws Exception {
        final String formToken = UUID.randomUUID().toString();

        final String first = submit(formToken, INCIDENT_NAME);
        final String second = submit(formToken, INCIDENT_NAME);

        assertThat(second)
            .as("gensendelsen skal ende på den hændelse den første indsendelse oprettede")
            .isEqualTo(first);
        assertThat(createdIncidents()).isEqualTo(1);
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

        assertThat(createdIncidents()).isEqualTo(2);
    }

    /** Er hændelsen slettet igen, er tokenen ikke længere noget at pege brugeren hen på. */
    @Test
    public void formTokenWhoseIncidentIsGoneCreatesANewOne() {
        final MockHttpSession session = new MockHttpSession();
        final String formToken = UUID.randomUUID().toString();

        incidentController.createOrUpdateIncident(newIncident(), formToken, session);
        incidentDao.findAll().stream()
            .filter(incident -> INCIDENT_NAME.equals(incident.getName()))
            .forEach(incidentService::delete);

        incidentController.createOrUpdateIncident(newIncident(), formToken, session);

        assertThat(createdIncidents()).isEqualTo(1);
    }

    /** Uden feltet i formularen har serveren ikke noget at genkende gensendelsen på. */
    @Test
    public void theCreateFormCarriesAToken() throws Exception {
        final String html = mockMvc.perform(get("/incidents/logForm"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(Jsoup.parse(html).selectFirst("input[name=formToken]")).isNotNull();
    }

    /**
     * Sessionen ligger i databasen via spring-session-jdbc, ikke i servletbeholderen, så den findes
     * kun igen hvis kaldet bærer cookien med - præcis som browseren gør.
     */
    private String submit(final String formToken, final String name) throws Exception {
        final MockHttpServletRequestBuilder request = post("/incidents/log").with(csrf().asHeader())
            .param("name", name)
            .param("formToken", formToken);
        if (cookies.length > 0) {
            request.cookie(cookies);
        }

        final MockHttpServletResponse response = mockMvc.perform(request)
            .andExpect(status().is3xxRedirection())
            .andReturn().getResponse();

        if (response.getCookies().length > 0) {
            cookies = response.getCookies();
        }
        return response.getRedirectedUrl();
    }

    private static Incident newIncident() {
        final Incident incident = new Incident();
        incident.setName(INCIDENT_NAME);
        return incident;
    }

    private long createdIncidents() {
        return incidentsNamed(INCIDENT_NAME);
    }

    private long incidentsNamed(final String name) {
        return incidentDao.findAll().stream()
            .filter(incident -> name.equals(incident.getName()))
            .count();
    }
}
