package dk.digitalidentity.mapping;


import dk.digitalidentity.model.dto.RegisterDTO;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.enums.RegisterStatus;
import dk.digitalidentity.model.entity.grid.RegisterGrid;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RegisterMapper {

    default RegisterDTO toDTO(final RegisterGrid registerGrid) {
        return RegisterDTO.builder()
                .id(registerGrid.getId())
                .name(registerGrid.getName())
                .responsibleUsers(nullSafe(() -> registerGrid.getResponsibleUserNames(), ""))
                .responsibleOUs(nullSafe(() -> registerGrid.getResponsibleOUNames(), ""))
                .departments(nullSafe(() -> registerGrid.getDepartmentNames(), ""))
                .updatedAt(nullSafe(() -> registerGrid.getUpdatedAt().format(DK_DATE_FORMATTER)))
                .consequence(nullSafe(() -> registerGrid.getConsequence().getMessage(), ""))
                .status(nullSafe(() -> registerGrid.getStatus().getMessage(), ""))
                .risk(nullSafe(() -> registerGrid.getRisk().getMessage(), ""))
                .assetCount(registerGrid.getAssetCount())
                .build();
    }

    List<RegisterDTO> toDTO(final List<RegisterGrid> registers);

    default Register fromDTO(final RegisterDTO registerDTO) {
        final Register r = new Register();
        r.setId(registerDTO.getId());
        r.setName(registerDTO.getName());
        r.setPackageName(registerDTO.getPackageName());
        r.setDescription(registerDTO.getDescription());
        r.setGdprChoices(registerDTO.getGdprChoices());
        r.setStatus(registerDTO.getStatus() == null ? RegisterStatus.NOT_STARTED : RegisterStatus.valueOf(registerDTO.getStatus()));
        return r;
    }

}
