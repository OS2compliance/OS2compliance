package dk.digitalidentity.mapping;

import static dk.digitalidentity.util.NullSafe.nullSafe;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import dk.digitalidentity.model.dto.DBSAssetDTO;
import dk.digitalidentity.model.entity.grid.DBSAssetGrid;


@SuppressWarnings("Convert2MethodRef")
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DBSAssetMapper {

    default DBSAssetDTO toDTO(final DBSAssetGrid assetGrid) {
        return DBSAssetDTO.builder()
                .id(assetGrid.getId())
                .name(assetGrid.getName())
                .supplier(nullSafe(() -> assetGrid.getSupplier()))
                .lastSync(assetGrid.getLastSync())
                .assets(nullSafe(() -> assetGrid.getAssets()))
                .build();
    }

    List<DBSAssetDTO> toDTO(List<DBSAssetGrid> assetGrids);

}
