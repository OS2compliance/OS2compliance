package dk.digitalidentity.service;

import dk.digitalidentity.dao.EmailTemplateDao;
import dk.digitalidentity.model.entity.EmailTemplate;
import dk.digitalidentity.model.entity.enums.EmailTemplateType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {
    private final EmailTemplateDao emailTemplateDao;

    public List<EmailTemplate> findAll() {
        List<EmailTemplate> result = new ArrayList<>();

        for (EmailTemplateType type : EmailTemplateType.values()) {
            result.add(findByTemplateType(type));
        }

        return result;
    }

    public EmailTemplate findByTemplateType(EmailTemplateType type) {
        EmailTemplate template = emailTemplateDao.findByTemplateType(type);
        if (template == null) {
            template = new EmailTemplate();
            String title = "Overskrift";
            String message = "Besked";
            boolean enabled = true;

            switch (type) {
                case RISK_REMINDER:
                    title = "Påmindelse om at udfylde risikovurdering";
                    message = "<p>Kære {modtager}</p>" +
                        "<p>Du er blevet tildelt opgaven med navn: \"{objekt}\", da du er risikoejer på en ny risikovurdering.</p>" +
                        "<p>Du kan finde opgaven her: {link}";
                    break;
                case TASK_RESPONSIBLE:
                    title = "Du er tildelt ansvaret for en ny opgave.";
                    message = "<p>Kære {modtager}</p>" +
                        "<p>Du er blevet tildelt opgaven med navn: \"{objekt}\"" +
                        "<p>Du kan finde opgaven her: {link}";
                    break;
                case RISK_REPORT:
                    title = "Risikovurdering: {objekt}";
                    message = "<p>Kære {modtager}</p>" +
                        "<p>{afsender} har sendt dig risikovurderingen med titlen {objekt}</p>" +
                        "<p>Personen har skrevet denne besked til dig:</p><p>{besked}</p>";
                    break;
                case RISK_REPORT_TO_SIGN:
                    title = "Risikovurdering: {objekt}";
                    message = "<p>Kære {modtager}</p>" +
                        "<p>{afsender} har sendt dig risikovurderingen med titlen {objekt}</p>" +
                        "<p>Personen har skrevet denne besked til dig:</p><p>{besked}</p>" +
                        "<p>Rapporten skal signeres. Det kan du gøre ved at følge dette link: {link}</p>";
                    break;
                case TASK_REMINDER:
                    title = "Deadline Notifikation";
                    message = "<p>Kære {modtager}</p>" +
                        "<p>Din opgave {objekt} har deadline om {dage} dag(e).</p>" +
                        "<p>Du kan finde opgaven her: {link}</p>";
                    break;
                case INACTIVE_USERS:
                    title = "Nye inaktive ansvarlige";
                    message = "<p>En eller flere brugere med ansvar er blevet inaktive.</p>" +
                        "<p>Se de inaktive brugere og overfør deres ansvar her: {link}</p>" +
                        "<p>Nye inaktive brugere:</p>" +
                        "<p>{brugere}</p>";
                    break;
            }

            template.setTitle(title);
            template.setMessage(message);
            template.setTemplateType(type);
            template.setEnabled(enabled);

            template = emailTemplateDao.save(template);
        }

        return template;
    }

    public EmailTemplate save(EmailTemplate template) {
        return emailTemplateDao.save(template);
    }

    public EmailTemplate findById(long id) {
        return emailTemplateDao.findById(id).orElse(null);
    }
}
