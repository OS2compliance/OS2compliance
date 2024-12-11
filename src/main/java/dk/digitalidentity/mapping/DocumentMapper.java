package dk.digitalidentity.mapping;

import dk.digitalidentity.model.api.DocumentCreateEO;
import dk.digitalidentity.model.api.DocumentEO;
import dk.digitalidentity.model.api.PageEO;
import dk.digitalidentity.model.api.UserEO;
import dk.digitalidentity.model.api.UserWriteEO;
import dk.digitalidentity.model.dto.AssetDTO;
import dk.digitalidentity.model.dto.DocumentDTO;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.AssetGrid;
import dk.digitalidentity.model.entity.grid.DocumentGrid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DocumentMapper {

    default DocumentDTO toDTO(final DocumentGrid documentGrid) {
        return DocumentDTO.builder()
                .id(documentGrid.getId())
                .name(documentGrid.getName())
                .documentType(nullSafe(() -> documentGrid.getDocumentType().getMessage()))
                .documentTypeOrder(documentGrid.getDocumentTypeOrder())
                .responsibleUser(nullSafe(() -> documentGrid.getResponsibleUser().getName(), ""))
                .nextRevision(nullSafe(() -> documentGrid.getNextRevision().format(DK_DATE_FORMATTER)))
                .status(nullSafe(() -> documentGrid.getStatus().getMessage()))
                .statusOrder(documentGrid.getStatusOrder())
                .tags(nullSafe(() -> documentGrid.getTags()))
                .changeable(false)
                .build();
    }

    default DocumentDTO toDTO(final DocumentGrid documentGrid, boolean superuser, String principalUuid) {
        DocumentDTO documentDTO = toDTO(documentGrid);
        if(superuser || principalUuid.equals(documentGrid.getResponsibleUser().getUuid())) {
            documentDTO.setChangeable(true);
        }
        return documentDTO;
    }

    default List<DocumentDTO> toDTO(List<DocumentGrid> documentGrid) {
        List<DocumentDTO> documentDTOS = new ArrayList<>();
        documentGrid.forEach(a -> documentDTOS.add(toDTO(a)));
        return documentDTOS;
    }

    //provides a list of mapping that's set changeable to true if user is at least a superuser or uuid matches current user's uuid.
    default List<DocumentDTO> toDTO(List<DocumentGrid> documentGrid, boolean superuser, String principalUuid) {
        List<DocumentDTO> documentDTOS = new ArrayList<>();
        documentGrid.forEach(a -> documentDTOS.add(toDTO(a, superuser, principalUuid)));
        return documentDTOS;
    }

    UserEO toEO(User user);

    @Mappings({
            @Mapping(target = "userId", ignore = true),
            @Mapping(target = "name", ignore = true),
            @Mapping(target = "email", ignore = true),
            @Mapping(target = "active", ignore = true),
            @Mapping(target = "positions", ignore = true),
            @Mapping(target = "properties", ignore = true),
            @Mapping(target = "roles", ignore = true),
            @Mapping(target = "password", ignore = true),
            @Mapping(target = "passwordResetRequestDate", ignore = true),
            @Mapping(target = "passwordResetToken", ignore = true),
            @Mapping(target = "assetRoles", ignore = true),
            @Mapping(target = "note", ignore = true),
    })
    User fromEO(UserWriteEO userWriteEO);

    DocumentEO toEO(Document document);
    List<DocumentEO> toEO(List<Document> document);

    default PageEO<DocumentEO> toEO(final Page<Document> page) {
        return PageEO.<DocumentEO>builder()
                .content(toEO(page.getContent()))
                .count(page.getNumberOfElements())
                .totalCount(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .build();
    }

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "relationType", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "updatedBy", ignore = true),
            @Mapping(target = "properties", ignore = true),
            @Mapping(target = "tags", ignore = true),
            @Mapping(target = "deleted", ignore = true),
            @Mapping(target = "localizedEnums", ignore = true)
    })
    Document fromEO(DocumentCreateEO documentCreateEO);

}
