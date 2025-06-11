package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDTO {
    private Long id;
    private String name;
    private String supplier;
    private String assetType;
    private String responsibleUsers;
    private String updatedAt;
    private String assessment;
    private Integer assessmentOrder;
    private String assetStatus;
    private String assetCategory;
    private Integer assetCategoryOrder;
    private String kitos;
    private boolean hasThirdCountryTransfer;
    private int registers;
    private boolean changeable;
}
