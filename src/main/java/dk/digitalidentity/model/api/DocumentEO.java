package dk.digitalidentity.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import static dk.digitalidentity.model.api.Examples.DOCUMENT_DESCRIPTION_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.DOCUMENT_LINK_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.DOCUMENT_REVISION_INTERVAL_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.DOCUMENT_STATUS_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.DOCUMENT_TYPE_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.DOCUMENT_VERSION_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.SUPPLIER_NAME_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.VERSION_EXAMPLE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "Document")
public class DocumentEO {
    public enum DocumentStatus { NOT_STARTED, IN_PROGRESS, READY }
    public enum DocumentType {
        PROCEDURE, GUIDE, WORKFLOW, CONTRACT, DATA_PROCESSING_AGREEMENT,
        SUPERVISORY_REPORT, MANAGEMENT_REPORT, RISK_ASSESSMENT_REPORT, OTHER
    }
    public enum DocumentRevisionInterval {
        NONE, YEARLY, EVERY_SECOND_YEAR, EVERY_THIRD_YEAR
    }

    @NotNull
    @Schema(description = "Resource version, must always match current version when updating", example = VERSION_EXAMPLE, requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer version;
    @NotEmpty
    @Schema(description = "Name of the supplier", requiredMode = Schema.RequiredMode.REQUIRED, example = SUPPLIER_NAME_EXAMPLE)
    private String name;
    @Schema(description = "Responsible user")
    private UserEO responsibleUser;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = DOCUMENT_STATUS_EXAMPLE)
    private DocumentStatus status;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = DOCUMENT_TYPE_EXAMPLE)
    private DocumentType documentType;
    @Schema(description = "Document description", example = DOCUMENT_DESCRIPTION_EXAMPLE)
    private String description;
    @Schema(description = "Link for the document", example = DOCUMENT_LINK_EXAMPLE)
    private String link;
    @Schema(description = "Version of the document", example = DOCUMENT_VERSION_EXAMPLE)
    private String documentVersion;
    @Schema(description = "Revision interval", example = DOCUMENT_REVISION_INTERVAL_EXAMPLE)
    private DocumentRevisionInterval revisionInterval;
    @Schema(description = "Next revision")
    private LocalDate nextRevision;
}
