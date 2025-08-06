package dk.digitalidentity.event;


import dk.digitalidentity.model.entity.enums.EmailTemplateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent {
    public record EmailAttachement (String inputFilePath, String wantedFilenameInEmail) {}

    private String email;
    private String subject;
    private String message;
	private EmailTemplateType templateType;
    // NOTICE: Attachments will be deleted after the event has been processed
    @Builder.Default
    private List<EmailAttachement> attachments = new ArrayList<>();
}
