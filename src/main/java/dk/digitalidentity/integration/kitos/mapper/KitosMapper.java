package dk.digitalidentity.integration.kitos.mapper;

import dk.kitos.api.model.ArchivingRegistrationsResponseDTO;
import dk.kitos.api.model.ArchivingUpdateRequestDTO;
import dk.kitos.api.model.GDPRRegistrationsResponseDTO;
import dk.kitos.api.model.GDPRWriteRequestDTO;
import dk.kitos.api.model.GeneralDataResponseDTO;
import dk.kitos.api.model.GeneralDataUpdateRequestDTO;
import dk.kitos.api.model.IdentityNamePairResponseDTO;
import dk.kitos.api.model.ItSystemUsageResponseDTO;
import dk.kitos.api.model.LocalKLEDeviationsRequestDTO;
import dk.kitos.api.model.LocalKLEDeviationsResponseDTO;
import dk.kitos.api.model.OrganizationUsageResponseDTO;
import dk.kitos.api.model.OrganizationUsageWriteRequestDTO;
import dk.kitos.api.model.RoleAssignmentRequestDTO;
import dk.kitos.api.model.RoleAssignmentResponseDTO;
import dk.kitos.api.model.UpdateItSystemUsageRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KitosMapper {

    @Mapping(target = "mainContractUuid", source = "mainContract")
    @Mapping(target = "dataClassificationUuid", source = "dataClassification")
    GeneralDataUpdateRequestDTO toUpdateReq(final GeneralDataResponseDTO responseDTO);

    @Mapping(target = "typeUuid", source = "type")
    @Mapping(target = "locationUuid", source = "location")
    @Mapping(target = "testLocationUuid", source = "testLocation")
    @Mapping(target = "supplierOrganizationUuid", source = "supplier.uuid")
    ArchivingUpdateRequestDTO toUpdateReq(ArchivingRegistrationsResponseDTO responseDTO);

    @Mapping(target = "roleUuid", source = "role")
    @Mapping(target = "userUuid", source = "user")
    RoleAssignmentRequestDTO toUpdateReq(final RoleAssignmentResponseDTO responseDTO);

    @Mapping(target = "usingOrganizationUnitUuids", source = "usingOrganizationUnits")
    @Mapping(target = "responsibleOrganizationUnitUuid", source = "responsibleOrganizationUnit")
    OrganizationUsageWriteRequestDTO toUpdateReq(final OrganizationUsageResponseDTO responseDTO);

    @Mapping(target = "sensitivePersonDataUuids", source = "sensitivePersonData")
    @Mapping(target = "registeredDataCategoryUuids", source = "registeredDataCategories")
    GDPRWriteRequestDTO toUpdateReq(GDPRRegistrationsResponseDTO responseDTO);

    @Mapping(target = "removedKLEUuids", source = "removedKLE")
    @Mapping(target = "addedKLEUuids", source = "addedKLE")
    LocalKLEDeviationsRequestDTO toUpdateReq (LocalKLEDeviationsResponseDTO responseDTO);

    default UUID toUuid(IdentityNamePairResponseDTO inputList) {
        if (inputList == null) {
            return null;
        }
        return inputList.getUuid();
    }
    @Mapping(target = "localKleDeviations", source = "localKLEDeviations")
    UpdateItSystemUsageRequestDTO toUpdateReq(final ItSystemUsageResponseDTO responseDTO);

}
