package dk.digitalidentity.mapping;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.Constants.LOCAL_TZ_ID;
import static dk.digitalidentity.util.NullSafe.nullSafe;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import dk.digitalidentity.model.api.AssetCreateEO;
import dk.digitalidentity.model.api.AssetEO;
import dk.digitalidentity.model.api.PageEO;
import dk.digitalidentity.model.api.PropertyEO;
import dk.digitalidentity.model.api.SupplierShallowEO;
import dk.digitalidentity.model.api.SupplierWriteEO;
import dk.digitalidentity.model.api.UserWriteEO;
import dk.digitalidentity.model.dto.AssetDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.AssetGrid;


@SuppressWarnings("Convert2MethodRef")
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AssetMapper {

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
            .changeable(false)
            .build();
    }

    //provides a mapping that's set changeable to true if user is at least a superuser or uuid matches current user's uuid.
    default AssetDTO toDTO(final AssetGrid assetGrid, boolean superuser, String principalUuid) {
        AssetDTO assetDTO = toDTO(assetGrid);
        if(superuser || principalUuid.equals(assetGrid.getResponsibleUserUuids())) {
            assetDTO.setChangeable(true);
        }
        return assetDTO;
    }

    default List<AssetDTO> toDTO(List<AssetGrid> assetGrids) {
        List<AssetDTO> assetDTOS = new ArrayList<>();
        assetGrids.forEach(a -> assetDTOS.add(toDTO(a)));
        return assetDTOS;
    }

    //provides a list of mapping that's set changeable to true if user is at least a superuser or uuid matches current user's uuid.
    //List<AssetDTO> toDTO(List<AssetGrid> assetGrids, boolean superuser, String principalUuid);
    default List<AssetDTO> toDTO(List<AssetGrid> assetGrids, boolean superuser, String principalUuid) {
        List<AssetDTO> assetDTOS = new ArrayList<>();
        assetGrids.forEach(a -> assetDTOS.add(toDTO(a, superuser, principalUuid)));
        return assetDTOS;
    }

    default SupplierShallowEO toShallowEO(final AssetSupplierMapping mapping) {
        return SupplierShallowEO.builder()
            .id(mapping.getSupplier().getId())
            .name(mapping.getSupplier().getName())
            .build();
    }

    @Mappings({
        @Mapping(source = "responsibleUsers", target = "systemOwners"),
        @Mapping(source = "managers", target = "responsibleUsers"),
        @Mapping(source = "suppliers", target = "subSuppliers")
    })
    AssetEO toEO(Asset asset);

    List<AssetEO> toEO(List<Asset> asset);

    default PageEO<AssetEO> toEO(final Page<Asset> page) {
        return PageEO.<AssetEO>builder()
            .content(toEO(page.getContent()))
            .count(page.getNumberOfElements())
            .totalCount(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .page(page.getNumber())
            .build();
    }

    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "entity", ignore = true)
    })
    Property fromEO(final PropertyEO property);
    Set<Property> fromEO(final Set<PropertyEO> property);

    List<Supplier> fromEO(final List<SupplierWriteEO> suppliers);

    default Supplier fromWriteEO(final SupplierWriteEO supplierWriteEO) {
        final Supplier supplier = new Supplier();
        supplier.setId(supplierWriteEO.getId());
        return supplier;
    }

    default User fromEO(final UserWriteEO eo) {
        return User.builder().uuid(eo.getUuid()).build();
    }

    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "version", ignore = true),
        @Mapping(target = "relationType", ignore = true),
        @Mapping(target = "createdAt", ignore = true),
        @Mapping(target = "createdBy", ignore = true),
        @Mapping(target = "updatedAt", ignore = true),
        @Mapping(target = "updatedBy", ignore = true),
        @Mapping(target = "dataProcessing", ignore = true),
        @Mapping(target = "tia", ignore = true),
        @Mapping(target = "assetOversights", ignore = true),
        @Mapping(target = "responsibleUsers", source = "systemOwners"),
        @Mapping(target = "suppliers", ignore = true),
        @Mapping(target = "measures", ignore = true),
        @Mapping(target = "dpia", ignore = true),
        @Mapping(target = "dpiaScreening", ignore = true),
        @Mapping(target = "managers", source = "responsibleUsers"),
        @Mapping(target = "deleted", ignore = true),
        @Mapping(target = "localizedEnums", ignore = true),
        @Mapping(target = "threatAssessmentOptOut", ignore = true),
        @Mapping(target = "threatAssessmentOptOutReason", ignore = true),
        @Mapping(target = "dpiaOptOut", ignore = true),
        @Mapping(target = "dpiaOptOutReason", ignore = true),
        @Mapping(target = "oversightResponsibleUser", ignore = true),
        @Mapping(target = "roles", ignore = true)
    })
    Asset fromEO(AssetCreateEO assetCreateEO);

}
