package dk.digitalidentity.mapping;

import dk.digitalidentity.model.api.PageEO;
import dk.digitalidentity.model.api.PropertyEO;
import dk.digitalidentity.model.api.SupplierCreateEO;
import dk.digitalidentity.model.api.SupplierEO;
import dk.digitalidentity.model.api.SupplierShallowEO;
import dk.digitalidentity.model.api.UserWriteEO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.SupplierDTO;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static dk.digitalidentity.Constants.LOCAL_TZ_ID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SupplierMapper {

    SupplierDTO toDTO(final Supplier supplier);

    List<SupplierDTO> toDTO(final List<Supplier> suppliers);

    default PageDTO<SupplierDTO> toDTO(final Page<Supplier> suppliers) {
        return new PageDTO<>(suppliers.getTotalElements(), toDTO(suppliers.getContent()));
    }

    default OffsetDateTime map(final LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(LOCAL_TZ_ID).toOffsetDateTime();
    }

    PropertyEO toEO(final Property property);

    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "entity", ignore = true)
    })
    Property fromEO(final PropertyEO property);
    Set<Property> fromEO(final Set<PropertyEO> property);

    SupplierEO toEO(final Supplier supplier);

    List<SupplierEO> toEO(final List<Supplier> suppliers);

    SupplierShallowEO toShallowEO(Supplier supplier);

    default PageEO<SupplierEO> toEO(final Page<Supplier> page) {
        return PageEO.<SupplierEO>builder()
                .content(toEO(page.getContent()))
                .count(page.getNumberOfElements())
                .totalCount(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .build();
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
        @Mapping(target = "personalInfo", ignore = true),
        @Mapping(target = "dataProcessor", ignore = true),
        @Mapping(target = "assets", ignore = true),
        @Mapping(target = "deleted", ignore = true),
        @Mapping(target = "localizedEnums", ignore = true)
    })
    Supplier fromEO(final SupplierCreateEO supplierCreateEO);

}
