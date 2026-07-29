package dk.digitalidentity.service;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.IncidentDao;
import dk.digitalidentity.dao.IncidentFieldDao;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.dto.IncidentDateFilter;
import dk.digitalidentity.model.dto.IncidentQuery;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.IncidentField;
import dk.digitalidentity.model.entity.IncidentFieldResponse;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.IncidentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the query behind the incident log. The interesting cases all involve answers to custom
 * fields, which live in their own table and are reached through EXISTS subqueries.
 */
@Transactional
public class IncidentFilterTest extends BaseIntegrationTest {

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 50);

    @Autowired
    private IncidentService incidentService;
    @Autowired
    private IncidentFieldDao incidentFieldDao;
    @Autowired
    private IncidentDao incidentDao;
    @Autowired
    private UserDao userDao;

    private IncidentField occurredOn;
    private IncidentField location;
    private IncidentField reporters;

    @BeforeEach
    public void setup() {
        occurredOn = saveField(IncidentType.DATE, "Hændelsesdato", true);
        location = saveField(IncidentType.TEXT, "Sted", false);
        reporters = saveField(IncidentType.USERS, "Anmeldere", false);
    }

    @Test
    public void filtersOnTheChosenDateFieldRatherThanCreatedAt() {
        // Both incidents were typed in today, which is exactly the situation the customer is in after
        // re-keying their log: only the answer date tells them apart.
        saveIncident("Tabt telefon", response(occurredOn, LocalDate.of(2026, 3, 10)));
        saveIncident("Forkert modtager", response(occurredOn, LocalDate.of(2026, 5, 20)));

        Page<Incident> result = incidentService.findIncidents(
                query(new IncidentDateFilter(IncidentDateFilter.Target.FIELD, occurredOn.getId()),
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)),
                FIRST_PAGE);

        assertThat(result.getContent()).extracting(Incident::getName).containsExactly("Tabt telefon");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    public void dateRangeBoundsAreInclusive() {
        saveIncident("Første dag", response(occurredOn, LocalDate.of(2026, 3, 1)));
        saveIncident("Sidste dag", response(occurredOn, LocalDate.of(2026, 3, 31)));

        Page<Incident> result = incidentService.findIncidents(
                query(new IncidentDateFilter(IncidentDateFilter.Target.FIELD, occurredOn.getId()),
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)),
                FIRST_PAGE);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    public void pickingADateFieldWithoutARangeDoesNotHideAnything() {
        saveIncident("Med dato", response(occurredOn, LocalDate.of(2026, 3, 10)));
        saveIncident("Uden dato");

        Page<Incident> result = incidentService.findIncidents(
                query(new IncidentDateFilter(IncidentDateFilter.Target.FIELD, occurredOn.getId()), null, null),
                FIRST_PAGE);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    public void filtersOnACustomTextColumn() {
        saveIncident("Sag A", response(location, "Rådhuset"));
        saveIncident("Sag B", response(location, "Biblioteket"));

        Page<Incident> result = incidentService.findIncidents(
                new IncidentQuery(IncidentDateFilter.DEFAULT, null, null, null,
                        Map.of(), Map.of(location.getId(), "råd")),
                FIRST_PAGE);

        assertThat(result.getContent()).extracting(Incident::getName).containsExactly("Sag A");
    }

    @Test
    public void dropsAColumnFilterOnAFieldThatIsGone() {
        // The filter lives in the browser and there is no filter box left on screen for a deleted
        // field, so honouring it would leave the user with an empty log and no way to clear it.
        saveIncident("Sag A", response(location, "Rådhuset"));

        Page<Incident> result = incidentService.findIncidents(
                new IncidentQuery(IncidentDateFilter.DEFAULT, null, null, null,
                        Map.of(), Map.of(999_999L, "hvad som helst")),
                FIRST_PAGE);

        assertThat(result.getContent()).extracting(Incident::getName).containsExactly("Sag A");
    }

    @Test
    public void dropsAColumnFilterOnAFieldNoLongerShownAsAColumn() {
        // Clearing a field's overview name removes its column, and with it the filter box.
        IncidentField hidden = saveField(IncidentType.TEXT, "Skjult", false);
        hidden.setIndexColumnName(null);
        incidentFieldDao.save(hidden);
        saveIncident("Sag A", response(hidden, "Rådhuset"));

        Page<Incident> result = incidentService.findIncidents(
                new IncidentQuery(IncidentDateFilter.DEFAULT, null, null, null,
                        Map.of(), Map.of(hidden.getId(), "biblioteket")),
                FIRST_PAGE);

        assertThat(result.getContent()).extracting(Incident::getName).containsExactly("Sag A");
    }

    @Test
    public void combinesFiltersOnTwoDifferentFields() {
        // A join based implementation cannot satisfy both of these at once, because one response row
        // can only match one of them.
        saveIncident("Begge", response(location, "Rådhuset"), response(occurredOn, LocalDate.of(2026, 3, 10)));
        saveIncident("Kun sted", response(location, "Rådhuset"), response(occurredOn, LocalDate.of(2026, 9, 1)));

        Page<Incident> result = incidentService.findIncidents(
                new IncidentQuery(
                        new IncidentDateFilter(IncidentDateFilter.Target.FIELD, occurredOn.getId()),
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null,
                        Map.of(), Map.of(location.getId(), "Rådhuset")),
                FIRST_PAGE);

        assertThat(result.getContent()).extracting(Incident::getName).containsExactly("Begge");
    }

    @Test
    public void filtersOnAReferencedUserNameEvenWhenTheFieldHoldsSeveral() {
        // The previous native query used "uuid IN (comma separated column)", which in MariaDB only
        // matches when the field holds exactly one element.
        User alice = saveUser("11111111-1111-1111-1111-111111111111", "abc", "Alice Andersen");
        User bob = saveUser("22222222-2222-2222-2222-222222222222", "bbc", "Bob Bertelsen");

        saveIncident("Flere anmeldere", responseElements(reporters, alice.getUuid(), bob.getUuid()));
        saveIncident("Ingen anmeldere");

        Page<Incident> result = incidentService.findIncidents(
                new IncidentQuery(IncidentDateFilter.DEFAULT, null, null, null,
                        Map.of(), Map.of(reporters.getId(), "Bertelsen")),
                FIRST_PAGE);

        assertThat(result.getContent()).extracting(Incident::getName).containsExactly("Flere anmeldere");
    }

    @Test
    public void searchesAcrossTitleAndAnswers() {
        saveIncident("Rådhuset lukkede ned");
        saveIncident("Sag B", response(location, "Rådhuset"));
        saveIncident("Sag C", response(location, "Biblioteket"));

        Page<Incident> result = incidentService.findIncidents(
                new IncidentQuery(IncidentDateFilter.DEFAULT, null, null, "rådhuset",
                        Map.of(), Map.of()),
                FIRST_PAGE);

        assertThat(result.getContent()).extracting(Incident::getName)
                .containsExactlyInAnyOrder("Rådhuset lukkede ned", "Sag B");
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    public void searchDoesNotDuplicateAnIncidentThatMatchesSeveralAnswers() {
        saveIncident("Sag A", response(location, "Rådhuset"), response(occurredOn, LocalDate.of(2026, 3, 10)));

        Page<Incident> result = incidentService.findIncidents(
                new IncidentQuery(IncidentDateFilter.DEFAULT, null, null, "råd",
                        Map.of(), Map.of()),
                FIRST_PAGE);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    public void deletedIncidentsAreLeftOut() {
        Incident deleted = saveIncident("Slettet");
        deleted.setDeleted(true);
        incidentDao.save(deleted);
        saveIncident("Aktiv");

        Page<Incident> result = incidentService.findIncidents(
                query(IncidentDateFilter.DEFAULT, null, null), FIRST_PAGE);

        assertThat(result.getContent()).extracting(Incident::getName).containsExactly("Aktiv");
    }

    @Test
    public void fallsBackToCreatedWhenTheChosenDateFieldIsNoLongerADateField() {
        // The choice is remembered in the browser, so an administrator changing the field's type
        // must not turn the log into a blank page.
        saveIncident("Sag A", response(location, "Rådhuset"));

        Page<Incident> result = incidentService.findIncidents(
                query(new IncidentDateFilter(IncidentDateFilter.Target.FIELD, location.getId()),
                        LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1)),
                FIRST_PAGE);

        assertThat(result.getContent()).extracting(Incident::getName).containsExactly("Sag A");
    }

    @Test
    public void fallsBackToCreatedWhenTheChosenDateFieldIsGone() {
        saveIncident("Sag A");

        Page<Incident> result = incidentService.findIncidents(
                query(new IncidentDateFilter(IncidentDateFilter.Target.FIELD, 999_999L),
                        LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1)),
                FIRST_PAGE);

        assertThat(result.getContent()).extracting(Incident::getName).containsExactly("Sag A");
    }

    @Test
    public void onlyObligatoryDateFieldsAreOfferedAsFilters() {
        saveField(IncidentType.DATE, "Frivillig dato", false);

        List<IncidentField> dateFields = incidentService.getDateFields();

        assertThat(dateFields).extracting(IncidentField::getQuestion).containsExactly("Hændelsesdato");
    }

    private IncidentQuery query(final IncidentDateFilter dateFilter, final LocalDate from, final LocalDate to) {
        return new IncidentQuery(dateFilter, from, to, null, Map.of(), Map.of());
    }

    private IncidentField saveField(final IncidentType type, final String question, final boolean obligatory) {
        final IncidentField field = new IncidentField();
        field.setIncidentType(type);
        field.setQuestion(question);
        field.setIndexColumnName(question);
        field.setObligatoryAnswer(obligatory);
        return incidentFieldDao.save(field);
    }

    private Incident saveIncident(final String name, final IncidentFieldResponse... responses) {
        final Incident incident = new Incident();
        incident.setName(name);
        for (final IncidentFieldResponse response : responses) {
            response.setIncident(incident);
            incident.getResponses().add(response);
        }
        return incidentDao.save(incident);
    }

    private static IncidentFieldResponse response(final IncidentField field, final String answer) {
        return IncidentFieldResponse.builder()
                .incidentField(field)
                .incidentType(field.getIncidentType())
                .question(field.getQuestion())
                .answerText(answer)
                .build();
    }

    private static IncidentFieldResponse response(final IncidentField field, final LocalDate answer) {
        return IncidentFieldResponse.builder()
                .incidentField(field)
                .incidentType(field.getIncidentType())
                .question(field.getQuestion())
                .answerDate(answer)
                .build();
    }

    private static IncidentFieldResponse responseElements(final IncidentField field, final String... ids) {
        return IncidentFieldResponse.builder()
                .incidentField(field)
                .incidentType(field.getIncidentType())
                .question(field.getQuestion())
                .answerElementIds(List.of(ids))
                .build();
    }

    private User saveUser(final String uuid, final String userId, final String name) {
        return userDao.save(User.builder()
                .active(true)
                .uuid(uuid)
                .userId(userId)
                .name(name)
                .build());
    }
}
