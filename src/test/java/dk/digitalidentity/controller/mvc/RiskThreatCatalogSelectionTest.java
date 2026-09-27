package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.ThreatAssessmentDao;
import dk.digitalidentity.dao.ThreatAssessmentResponseDao;
import dk.digitalidentity.dao.ThreatCatalogDao;
import dk.digitalidentity.dao.ThreatCatalogThreatDao;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Trusselskatalog-feltet fyldes med de synlige kataloger. Var et tilknyttet katalog skjult, kunne formularen
 * ikke sende det retur, og næste gem fravalgte det - hvilket sletter alle besvarelser under det.
 */
@Transactional
@AutoConfigureMockMvc
public class RiskThreatCatalogSelectionTest extends BaseIntegrationTest {
    private static final String CATALOG_IDENTIFIER = "skjult-katalog";

    @Autowired private MockMvc mockMvc;
    @Autowired private ThreatCatalogDao catalogDao;
    @Autowired private ThreatCatalogThreatDao threatDao;
    @Autowired private ThreatAssessmentDao assessmentDao;
    @Autowired private ThreatAssessmentResponseDao responseDao;
    @PersistenceContext private EntityManager em;

    private long assessmentId;

    @BeforeEach
    void setup() {
        final ThreatCatalog newCatalog = new ThreatCatalog();
        newCatalog.setIdentifier(CATALOG_IDENTIFIER);
        newCatalog.setName("Skjult katalog");
        newCatalog.setHidden(true);
        final ThreatCatalog catalog = catalogDao.saveAndFlush(newCatalog);

        final ThreatCatalogThreat newThreat = new ThreatCatalogThreat();
        newThreat.setIdentifier("skjult-katalog-trussel");
        newThreat.setThreatCatalog(catalog);
        newThreat.setThreatType("En type");
        newThreat.setDescription("En trussel");
        newThreat.setSortKey(1L);
        final ThreatCatalogThreat threat = threatDao.saveAndFlush(newThreat);

        final ThreatAssessment assessment = new ThreatAssessment();
        assessment.setName("Risikovurdering med skjult katalog");
        assessment.setThreatAssessmentType(ThreatAssessmentType.SCENARIO);
        assessment.setRegistered(true);
        assessment.setThreatCatalogs(new ArrayList<>(List.of(catalog)));
        assessment.setPresentAtMeeting(new ArrayList<>());
        assessmentDao.save(assessment);

        final ThreatAssessmentResponse response = new ThreatAssessmentResponse();
        response.setName("En trussel");
        response.setThreatAssessment(assessment);
        response.setThreatCatalogThreat(threat);
        response.setProbability(4);
        response.setConfidentialityRegistered(3);
        responseDao.save(response);

        em.flush();
        em.clear();

        assessmentId = assessment.getId();
    }

    @Test
    public void editDialogOffersTheAssignedCatalogEvenThoughItIsHidden() throws Exception {
        assertThat(catalogOption(html("/risks/" + assessmentId + "/edit"))).contains("selected");
    }

    @Test
    public void viewPageOffersTheAssignedCatalogEvenThoughItIsHidden() throws Exception {
        assertThat(catalogOption(html("/risks/" + assessmentId))).contains("selected");
    }

    /** Kom katalogfeltet ikke med i formularen, er det ikke et fravalg, og så må intet slettes. */
    @Test
    public void savingWithoutTheCatalogFieldKeepsCatalogsAndResponses() throws Exception {
        mockMvc.perform(post("/risks/" + assessmentId + "/edit").with(csrf().asHeader())
                        .param("name", "Risikovurdering med skjult katalog")
                        .param("threatAssessmentType", "SCENARIO"))
                .andExpect(status().is3xxRedirection());
        em.flush();
        em.clear();

        assertThat(assessmentDao.findById(assessmentId).orElseThrow().getThreatCatalogs()).hasSize(1);
        assertThat(responseCount()).isEqualTo(1);
    }

    @Test
    public void updateCatalogsWithoutTheCatalogFieldKeepsCatalogsAndResponses() throws Exception {
        mockMvc.perform(post("/risks/" + assessmentId + "/update-catalogs").with(csrf().asHeader()))
                .andExpect(status().is3xxRedirection());
        em.flush();
        em.clear();

        assertThat(assessmentDao.findById(assessmentId).orElseThrow().getThreatCatalogs()).hasSize(1);
        assertThat(responseCount()).isEqualTo(1);
    }

    /** Et bevidst fravalg skal stadig fjerne kataloget og dets besvarelser. */
    @Test
    public void deselectingTheCatalogStillRemovesItAndItsResponses() throws Exception {
        mockMvc.perform(post("/risks/" + assessmentId + "/update-catalogs").with(csrf().asHeader())
                        .param("_threatCatalogs", "on"))
                .andExpect(status().is3xxRedirection());
        em.flush();
        em.clear();

        assertThat(assessmentDao.findById(assessmentId).orElseThrow().getThreatCatalogs()).isEmpty();
        assertThat(responseCount()).isZero();
    }

    private long responseCount() {
        return em.createQuery("SELECT COUNT(r) FROM ThreatAssessmentResponse r WHERE r.threatAssessment.id = :id", Long.class)
                .setParameter("id", assessmentId)
                .getSingleResult();
    }

    private String html(final String url) throws Exception {
        return mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String catalogOption(final String html) {
        final Matcher matcher = Pattern.compile("<option[^>]*" + CATALOG_IDENTIFIER + "[^>]*>").matcher(html);
        return matcher.find() ? matcher.group() : "<option ikke fundet>";
    }
}
