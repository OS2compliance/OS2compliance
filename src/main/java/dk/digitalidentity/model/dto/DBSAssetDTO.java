package dk.digitalidentity.model.dto;

import java.util.List;

import dk.digitalidentity.model.entity.Asset;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DBSAssetDTO {
    private Long id;
    private String name;
    private List<Asset> assets;
    private String supplier;
}
