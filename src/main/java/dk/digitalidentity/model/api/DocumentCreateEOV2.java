package dk.digitalidentity.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import static dk.digitalidentity.model.api.Examples.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "Document")
public class DocumentCreateEOV2 {
	@NotEmpty
	@Schema(description = "Name of the supplier", requiredMode = Schema.RequiredMode.REQUIRED, example = SUPPLIER_NAME_EXAMPLE)
	private String name;
	@Schema(description = "Responsible user")
	private UserWriteEO responsibleUser;
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = DOCUMENT_STATUS_EXAMPLE)
	private DocumentEOV2.DocumentStatus status;
	@Schema(description = "Document type identifier", requiredMode = Schema.RequiredMode.REQUIRED, example = DOCUMENT_TYPE_NEW_EXAMPLE)
	private String documentTypeIdentifier;
	@Schema(description = "Document description", example = DOCUMENT_DESCRIPTION_EXAMPLE)
	private String description;
	@Schema(description = "Link for the document", example = DOCUMENT_LINK_EXAMPLE)
	private String link;
	@Schema(description = "Version of the document", example = DOCUMENT_VERSION_EXAMPLE)
	private String documentVersion;
	@Schema(description = "Revision interval", example = DOCUMENT_REVISION_INTERVAL_EXAMPLE)
	private DocumentEOV2.DocumentRevisionInterval revisionInterval;
	@Schema(description = "Next revision")
	private LocalDate nextRevision;
}