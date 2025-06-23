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
import static dk.digitalidentity.model.api.Examples.ASSET_EMERGENCY_LINK_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_NAME_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_PRODUCT_LINKS_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_RE_ESTAB_LINK_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_SOC_CRITICALITY_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.ASSET_STATUS_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.VERSION_EXAMPLE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "AssetCreate")
public class AssetUpdateEO {
    @NotNull
    @Schema(description = "Resource version, must always match current version when updating", example = VERSION_EXAMPLE, requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer version;
    @NotEmpty
    @Schema(description = "Name of the asset", requiredMode = Schema.RequiredMode.REQUIRED, example = ASSET_NAME_EXAMPLE)
    private String name;
    @Schema(description = "System owners")
    private List<UserWriteEO> systemOwners;
    @Schema(description = "Asset description", example = ASSET_DESCRIPTION_EXAMPLE)
    private String description;
    @Schema(description = "Type of the asset")
    private AssetTypeUpdateEO assetType;
    @Schema(description = "Status of the data processing agreement", example = ASSET_DPA_EXAMPLE)
    private AssetEO.DataProcessingAgreementStatus dataProcessingAgreementStatus;
    @Schema(description = "Date of the data processing agreement", example = ASSET_DPA_DATE_EXAMPLE)
    private LocalDate dataProcessingAgreementDate;
    @Schema(description = "Link to data processing agreement", example = ASSET_DPA_LINK_EXAMPLE)
    private String dataProcessingAgreementLink;
    @Schema(description = "Supervising model", example = ASSET_DPA_SUPERVISION_EXAMPLE)
    private AssetEO.ChoiceOfSupervisionModel supervisoryModel;
    @Schema(description = "Next inspection setting", example = ASSET_DPA_INSPECTION_SETTING_EXAMPLE)
    private AssetEO.NextInspection nextInspection;
    @Schema(description = "Next inspection date", example = ASSET_DPA_DATE_EXAMPLE)
    private LocalDate nextInspectionDate;
    @Schema(description = "Status of the asset", example = ASSET_STATUS_EXAMPLE)
    private AssetEO.AssetStatus assetStatus;
    @Schema(description = "Criticality of the asset", example = ASSET_CRITICALITY_EXAMPLE)
    private AssetEO.Criticality criticality;
    @Schema(description = "Socially critical flag", example = ASSET_SOC_CRITICALITY_EXAMPLE)
    private boolean sociallyCritical;
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
    private SupplierWriteEO supplier;
    @Schema(description = "Sub suppliers")
    private List<SupplierWriteEO> subSuppliers;
    @Schema(description = "Responsible users")
    private List<UserWriteEO> responsibleUsers;
    @Schema(description = "Custom properties that can be set on the asset, can be external identifier flags etc.")
    private Set<PropertyEO> properties;
	@Schema(description = "Product links", example = ASSET_PRODUCT_LINKS_EXAMPLE)
	private List<String> productLinks;
}
