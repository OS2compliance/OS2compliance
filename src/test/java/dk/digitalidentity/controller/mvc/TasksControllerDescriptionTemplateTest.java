package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.service.ChoiceService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Skabelonvalget er et almindeligt select-felt. Uden en tom valgmulighed sender browseren den første
 * skabelon med, og da {@link Task#getDescription()} viser skabelonens tekst frem for opgavens egen,
 * forsvinder den beskrivelse brugeren har skrevet.
 */
@Transactional
@AutoConfigureMockMvc
public class TasksControllerDescriptionTemplateTest extends BaseIntegrationTest {

    private static final String TEST_USER_UUID = "ff6fc101-aeb2-486e-8d39-5d8e718abdec";

    @Autowired private MockMvc mockMvc;
    @Autowired private TasksController tasksController;
    @Autowired private TaskDao taskDao;
    @Autowired private UserDao userDao;
    @Autowired private ChoiceValueDao choiceValueDao;
    @Autowired private ChoiceService choiceService;
    @PersistenceContext private EntityManager em;

    @Test
    public void formSubmitsNoTemplateUntilOneIsPicked() throws Exception {
        persistTemplate();

        final Elements selected = renderedTemplateSelect(get("/tasks/form"))
            .select("option[selected]");

        assertThat(selected)
            .as("browseren vælger den første valgmulighed når ingen er markeret")
            .hasSize(1);
        assertThat(selected.first().attr("value")).isEmpty();
    }

    @Test
    public void formPreselectsTheTemplateTheTaskAlreadyHas() throws Exception {
        final ChoiceValue template = persistTemplate();
        final Task task = persistTask(template, "Min egen beskrivelse");

        final Elements selected = renderedTemplateSelect(get("/tasks/form").param("id", task.getId().toString()))
            .select("option[selected]");

        assertThat(selected).hasSize(1);
        assertThat(selected.first().attr("value")).isEqualTo(template.getId().toString());
    }

    /** Beskrivelsesfeltet viser skabelonteksten, så opgavens egen tekst følger med som data-attribut. */
    @Test
    public void formCarriesTheTasksOwnDescription() throws Exception {
        final Task task = persistTask(persistTemplate(), "Min egen beskrivelse");

        final String html = renderedForm(get("/tasks/form").param("id", task.getId().toString()));

        assertThat(Jsoup.parse(html).selectFirst("#taskEditFormdescription").attr("data-own-description"))
            .isEqualTo("Min egen beskrivelse");
    }

    @Test
    public void clearsDescriptionWhenTheFieldIsSubmittedEmpty() {
        final Long taskId = persistTask(null, "Min egen beskrivelse").getId();

        tasksController.formEdit(submittedForm(taskId, null, ""), false);
        em.flush();
        em.clear();

        assertThat(taskDao.findById(taskId).orElseThrow().getOwnDescription()).isEmpty();
    }

    /** Skabelonen låser beskrivelsesfeltet i browseren, og låste felter sendes slet ikke med. */
    @Test
    public void keepsStoredDescriptionWhenTheTemplateLocksTheField() {
        final ChoiceValue template = persistTemplate();
        final Long taskId = persistTask(template, "Min egen beskrivelse").getId();

        tasksController.formEdit(submittedForm(taskId, template, null), false);
        em.flush();
        em.clear();

        final Task reloaded = taskDao.findById(taskId).orElseThrow();
        assertThat(reloaded.getTaskDescriptionTemplate()).isNotNull();
        assertThat(reloaded.getTaskDescriptionTemplate().getId()).isEqualTo(template.getId());
        assertThat(reloaded.getOwnDescription())
            .as("den skrevne beskrivelse skal stadig være der hvis skabelonen fravælges igen")
            .isEqualTo("Min egen beskrivelse");
    }

    @Test
    public void savesDescriptionWhenTheTemplateIsCleared() {
        final ChoiceValue template = persistTemplate();
        final Long taskId = persistTask(template, "Min egen beskrivelse").getId();

        tasksController.formEdit(submittedForm(taskId, null, "Ny beskrivelse"), false);
        em.flush();
        em.clear();

        final Task reloaded = taskDao.findById(taskId).orElseThrow();
        assertThat(reloaded.getTaskDescriptionTemplate()).isNull();
        assertThat(reloaded.getDescription()).isEqualTo("Ny beskrivelse");
    }

    private String renderedForm(final MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    private Element renderedTemplateSelect(final MockHttpServletRequestBuilder request) throws Exception {
        final Element select = Jsoup.parse(renderedForm(request)).selectFirst("select[name=taskDescriptionTemplate]");
        assertThat(select).as("skabelonfeltet skal være med i formularen").isNotNull();
        return select;
    }

    private Task submittedForm(final Long taskId, final ChoiceValue template, final String description) {
        final Task form = new Task();
        form.setId(taskId);
        form.setName("Opgave med skabelon");
        form.setNextDeadline(LocalDate.now().plusDays(7));
        form.setResponsibleUsers(new HashSet<>(Set.of(testUser())));
        form.setTaskDescriptionTemplate(template);
        form.setDescription(description);
        return form;
    }

    private Task persistTask(final ChoiceValue template, final String description) {
        final Task task = new Task();
        task.setName("Opgave med skabelon");
        task.setNextDeadline(LocalDate.now().plusDays(7));
        task.setResponsibleUsers(new HashSet<>(Set.of(testUser())));
        task.setTaskDescriptionTemplate(template);
        task.setDescription(description);
        return taskDao.save(task);
    }

    private ChoiceValue persistTemplate() {
        final ChoiceValue template = choiceValueDao.save(ChoiceValue.builder()
            .caption("Skabelon til opgavebeskrivelse")
            .identifier("test-task-description-template")
            .description("Tekst fra skabelonen")
            .build());

        final ChoiceList list = choiceService.findChoiceList("task-description-template")
            .orElseGet(() -> choiceService.save(ChoiceList.builder()
                .identifier("task-description-template")
                .name("Opgavebeskrivelses skabelon")
                .multiSelect(false)
                .customizable(true)
                .build()));
        list.getValues().add(template);
        choiceService.save(list);
        return template;
    }

    private User testUser() {
        return userDao.findById(TEST_USER_UUID)
            .orElseGet(() -> userDao.save(User.builder()
                .uuid(TEST_USER_UUID)
                .userId("testbruger")
                .name("Testbruger")
                .active(true)
                .build()));
    }
}
