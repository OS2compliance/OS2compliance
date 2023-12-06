package dk.digitalidentity.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static dk.digitalidentity.model.api.Examples.ASSET_CONTRACT_LINK_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_CONTRACT_TERMINATION_NOTICE_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_CRITICALITY_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_DESCRIPTION_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_DPA_DATE_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_DPA_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_DPA_INSPECTION_SETTING_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_DPA_LINK_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_DPA_SUPERVISION_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_DPA_SUPERVISION_FREQ_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_EMERGENCY_LINK_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_NAME_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_PRODUCT_LINK_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_RE_ESTAB_LINK_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_SOC_CRITICALITY_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_STATUS_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_TYPE_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ID_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.OFFSET_DATE_TIME_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.USER_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.VERSION_EXAMPLE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "Asset")
public class AssetEO {
    public enum AssetType {
        IT_SYSTEM, MODULE, SERVER
    }
    public enum DataProcessingAgreementStatus {
        YES, NO, ON_GOING, NOT_RELEVANT
    }
    public enum ChoiceOfSupervisionModel {
        SELFCONTROL, PHYSICAL_SUPERVISION, ISAE_3000, ISAE_3402, ISRS_4400, SUPERVISION_JUSTIFIED_SUSPICION,
        MANAGEMENT_STATEMENT, WRITTEN_CONTROL, SUPERVISION_FORM_DECLARATION_OF_FAITH_AND_LAWS,
        SWORN_STATEMENT, INDEPENDENT_AUDIT, SOC_STATEMENT
    }
    public enum NextInspection {
        DATE, MONTH, QUARTER, HALF_YEAR, YEAR, EVERY_2_YEARS, EVERY_3_YEARS
    }
    public enum AssetStatus {
        READY, ON_GOING, NOT_STARTED
    }
    public enum Criticality {
        CRITICAL, NON_CRITICAL
    }
    @Schema(description = "Internal ID in OS2compliance", accessMode = Schema.AccessMode.READ_ONLY, example = ID_EXAMPLE)
    private Long id;
    @Schema(description = "Resource version, must always match current version when updating", example = VERSION_EXAMPLE)
    private Integer version;
    @Schema(description = "Resource creation date", accessMode = Schema.AccessMode.READ_ONLY, example = OFFSET_DATE_TIME_EXAMPLE)
    private OffsetDateTime createdAt;
    @Schema(description = "Name of creator", accessMode = Schema.AccessMode.READ_ONLY, example = USER_EXAMPLE)
    private String createdBy;
    @Schema(description = "Resource update date", accessMode = Schema.AccessMode.READ_ONLY, example = OFFSET_DATE_TIME_EXAMPLE)
    private OffsetDateTime updatedAt;
    @Schema(description = "Name of updater", accessMode = Schema.AccessMode.READ_ONLY, example = USER_EXAMPLE)
    private String updatedBy;
    @Schema(description = "Name of the asset", requiredMode = Schema.RequiredMode.REQUIRED, example = ASSET_NAME_EXAMPLE)
    private String name;
    @Schema(description = "System owner")
    private UserEO systemOwner;
    @Schema(description = "Asset description", example = ASSET_DESCRIPTION_EXAMPLE)
    private String description;
    @Schema(description = "Type of the asset", example = ASSET_TYPE_EXAMPLE)
    private AssetType assetType;
    @Schema(description = "Status of the data processing agreement", example = ASSET_DPA_EXAMPLE)
    private DataProcessingAgreementStatus dataProcessingAgreementStatus;
    @Schema(description = "Date of the data processing agreement", example = ASSET_DPA_DATE_EXAMPLE)
    private LocalDate dataProcessingAgreementDate;
    @Schema(description = "Link to data processing agreement", example = ASSET_DPA_LINK_EXAMPLE)
    private String dataProcessingAgreementLink;
    @Schema(description = "Supervising model", example = ASSET_DPA_SUPERVISION_EXAMPLE)
    private ChoiceOfSupervisionModel supervisoryModel;
    @Schema(description = "Next inspection setting", example = ASSET_DPA_INSPECTION_SETTING_EXAMPLE)
    private NextInspection nextInspection;
    @Schema(description = "Next inspection date", example = ASSET_DPA_DATE_EXAMPLE)
    private LocalDate nextInspectionDate;
    @Schema(description = "Status of the asset", example = ASSET_STATUS_EXAMPLE)
    private AssetStatus assetStatus;
    @Schema(description = "Criticality of the asset", example = ASSET_CRITICALITY_EXAMPLE)
    private Criticality criticality;
    @Schema(description = "Socially critical flag", example = ASSET_SOC_CRITICALITY_EXAMPLE)
    private boolean sociallyCritical;
    @Schema(description = "Product link", example = ASSET_PRODUCT_LINK_EXAMPLE)
    private String productLink;
    @Schema(description = "Emergency plan link", example = ASSET_EMERGENCY_LINK_EXAMPLE)
    private String emergencyPlanLink;
    @Schema(description = "Reestablishment plan link", example = ASSET_RE_ESTAB_LINK_EXAMPLE)
    private String reEstablishmentPlanLink;
    @Schema(description = "Contract link", example = ASSET_CONTRACT_LINK_EXAMPLE)
    private String contractLink;
    @Schema(description = "Contract date", example = ASSET_DPA_DATE_EXAMPLE)
    private LocalDate contractDate;
    @Schema(description = "Contract termination date", example = ASSET_DPA_DATE_EXAMPLE)
    private LocalDate contractTermination;
    @Schema(description = "Contract termination notice", example = ASSET_CONTRACT_TERMINATION_NOTICE_EXAMPLE)
    private String terminationNotice;
    @Schema(description = "Archiving flag", example = "false")
    private boolean archive;
    @Schema(description = "Supplier")
    private SupplierShallowEO supplier;
    @Schema(description = "Sub suppliers")
    private List<SupplierShallowEO> subSuppliers;
    @Schema(description = "Responsible users")
    private List<UserEO> responsibleUsers;
    @Schema(description = "Custom properties that can be set on the asset, can be external identifier flags etc.")
    private Set<PropertyEO> properties;
}
