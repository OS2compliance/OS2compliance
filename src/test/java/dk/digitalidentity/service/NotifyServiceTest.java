package dk.digitalidentity.service;

import dk.digitalidentity.event.EmailEvent;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.samlmodule.config.settings.DISAML_Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * Unit tests for {@link NotifyService}
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {NotifyService.class})
@EnableConfigurationProperties(value = DISAML_Configuration.class)
@RecordApplicationEvents
public class NotifyServiceTest {
    @Autowired
    private NotifyService notifyService;
    @Autowired
    private ApplicationEvents events;
    @MockBean
    private TaskService taskService;

    @Test
    public void canSendNotification() {
        // Given
        final Task dummyTask = createDummyTask();
        doReturn(Optional.of(dummyTask)).when(taskService).findById(any());

        // When
        notifyService.notifyTask(dummyTask.getId());

        // Then
        assertThat(dummyTask.getNotifyResponsible()).isTrue();
        final EmailEvent emailEvent = events.stream(EmailEvent.class)
            .findFirst().orElseGet(() -> fail("Event not received"));
        assertThat(emailEvent.getEmail()).isEqualTo(dummyTask.getResponsibleUser().getEmail());
        assertThat(emailEvent.getSubject()).isEqualTo("Deadline Notifikation");
        assertThat(emailEvent.getMessage()).isEqualTo(expectedMessage());
    }

    @Test
    public void willNotSendNotificationMultipleTimes() {
        // Given
        final Task dummyTask = createDummyTask();
        doReturn(Optional.of(dummyTask)).when(taskService).findById(any());

        // When
        IntStream.of(10).forEach(i -> {
            notifyService.notifyTask(dummyTask.getId());
        });

        // Then
        assertThat(dummyTask.getNotifyResponsible()).isTrue();

        final EmailEvent emailEvent = events.stream(EmailEvent.class)
            .findFirst().orElseGet(() -> fail("Event not received"));
        assertThat(emailEvent.getEmail()).isEqualTo(dummyTask.getResponsibleUser().getEmail());
        assertThat(emailEvent.getSubject()).isEqualTo("Deadline Notifikation");
        assertThat(emailEvent.getMessage()).isEqualTo(expectedMessage());
    }

    private static String expectedMessage() {
        return "Din opgave opgave navn har deadline om " + ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.now().plusDays(5)) + " dag(e).\n" +
            " Du kan finde opgaven her: https://os2compliance:8343/tasks/1";
    }

    private static Task createDummyTask() {
        final Task t = new Task();
        t.setId(1L);
        t.setName("opgave navn");
        t.setHasNotifiedResponsible(false);
        t.setNextDeadline(LocalDate.now().plusDays(5));
        t.setResponsibleUser(User.builder()
                .email("dev@null")
            .build());
        return t;
    }

}
