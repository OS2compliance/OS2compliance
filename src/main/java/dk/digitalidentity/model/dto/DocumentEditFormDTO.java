package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.DocumentRevisionInterval;
import dk.digitalidentity.model.entity.enums.DocumentStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DocumentEditFormDTO {
	@NotNull
	private Long id;

	@NotEmpty
	private String name;

	private String description;

	@NotNull
	private Long documentTypeId;

	private String documentVersion;

	@NotNull
	private DocumentStatus status;

	private String link;

	@NotNull
	private DocumentRevisionInterval revisionInterval;

	@DateTimeFormat(pattern = "dd/MM-yyyy")
	private LocalDate nextRevision;

	@NotNull
	private User responsibleUser;

	@NotNull
	private boolean includeInYearWheel;

}
