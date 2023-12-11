package dk.digitalidentity.service;

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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link NotifyService}
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {NotifyService.class})
@EnableConfigurationProperties(value = DISAML_Configuration.class)
public class NotifyServiceTest {
    @Autowired
    private NotifyService notifyService;
    @MockBean
    private MailService mailService;

    @Test
    public void canSendNotification() {
        // Given
        final Task dummyTask = createDummyTask();

        // When
        notifyService.notifyTask(dummyTask);

        // Then
        verify(mailService, times(1)).sendMessage(eq(dummyTask.getResponsibleUser().getEmail()),
            eq("Deadline Notifikation"), eq(expectedMessage()));
        assertThat(dummyTask.getNotifyResponsible()).isTrue();
    }

    @Test
    public void willNotSendNotificationMultipleTimes() {
        // Given
        final Task dummyTask = createDummyTask();

        // When
        IntStream.of(10).forEach(i -> {
            notifyService.notifyTask(dummyTask);
        });

        // Then
        verify(mailService, times(1)).sendMessage(eq(dummyTask.getResponsibleUser().getEmail()),
            eq("Deadline Notifikation"), eq(expectedMessage()));
        assertThat(dummyTask.getNotifyResponsible()).isTrue();
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
