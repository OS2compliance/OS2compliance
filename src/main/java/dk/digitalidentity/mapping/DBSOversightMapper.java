package dk.digitalidentity.mapping;

import static dk.digitalidentity.util.NullSafe.nullSafe;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import dk.digitalidentity.model.dto.DBSAssetDTO;
import dk.digitalidentity.model.dto.DBSOversightDTO;
import dk.digitalidentity.model.entity.grid.DBSOversightGrid;

@SuppressWarnings("Convert2MethodRef")
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DBSOversightMapper {

	default DBSOversightDTO toDTO(final DBSOversightGrid oversightGrid) {
        return DBSOversightDTO.builder()
                .id(oversightGrid.getId())
                .name(oversightGrid.getName())
                .supplier(nullSafe(() -> oversightGrid.getSupplier()))
                .supervisoryModel(nullSafe(() -> oversightGrid.getSupervisoryModel().getMessage()))
                .dbsAssets(nullSafe( () -> oversightGrid.getDbsAssets().stream().map(a -> DBSAssetDTO.builder().id(a.getId()).name(a.getName()).build()).toList()))
                .oversightResponsible(nullSafe(()-> "(" + oversightGrid.getOversightResponsible().getUserId() + ") " + oversightGrid.getOversightResponsible().getName()))
                .lastInspection(oversightGrid.getLastInspection())
                .lastInspectionStatus(nullSafe(() -> oversightGrid.getLastInspectionStatus().getMessage()))
                .outstandingSince(oversightGrid.getOutstandingSince())
                .build();
    }

    List<DBSOversightDTO> toDTO(List<DBSOversightGrid> oversightGrids);

}
