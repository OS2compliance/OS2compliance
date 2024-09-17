package dk.digitalidentity.mapping;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.Constants.LOCAL_TZ_ID;
import static dk.digitalidentity.util.NullSafe.nullSafe;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import dk.digitalidentity.model.dto.AssetDTO;
import dk.digitalidentity.model.dto.DBSAssetDTO;
import dk.digitalidentity.model.entity.grid.AssetGrid;
import dk.digitalidentity.model.entity.grid.DBSAssetGrid;


@SuppressWarnings("Convert2MethodRef")
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DBSAssetMapper {

    default OffsetDateTime map(final LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(LOCAL_TZ_ID).toOffsetDateTime();
    }

    default AssetDTO toDTO(final AssetGrid assetGrid) {
        return AssetDTO.builder()
                .id(assetGrid.getId())
                .name(assetGrid.getName())
                .supplier(nullSafe(() -> assetGrid.getSupplier()))
                .assetType(nullSafe(() -> assetGrid.getAssetType().getMessage()))
                .responsibleUsers(nullSafe(() -> assetGrid.getResponsibleUserNames()))
                .updatedAt(nullSafe(() -> assetGrid.getUpdatedAt().format(DK_DATE_FORMATTER)))
                .assessment(nullSafe(() -> assetGrid.getAssessment().getMessage()))
                .assessmentOrder(assetGrid.getAssessmentOrder())
                .assetStatus(nullSafe(() -> assetGrid.getAssetStatus().getMessage()))

                .kitos(nullSafe(() -> BooleanUtils.toStringTrueFalse(assetGrid.isKitos())))
                .registers(nullSafe(() -> assetGrid.getRegisters()))
                .hasThirdCountryTransfer(assetGrid.isHasThirdCountryTransfer())
                .build();
    }

    List<DBSAssetDTO> toDTO(List<DBSAssetGrid> assetGrids);

}
