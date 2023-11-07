package dk.digitalidentity.mapping;

import dk.digitalidentity.model.api.DocumentCreateEO;
import dk.digitalidentity.model.api.DocumentEO;
import dk.digitalidentity.model.api.PageEO;
import dk.digitalidentity.model.api.UserEO;
import dk.digitalidentity.model.api.UserWriteEO;
import dk.digitalidentity.model.dto.DocumentDTO;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.DocumentGrid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

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
                .responsibleUser(nullSafe(() -> documentGrid.getResponsibleUser().getName(), ""))
                .nextRevision(nullSafe(() -> documentGrid.getNextRevision().format(DK_DATE_FORMATTER)))
                .status(nullSafe(() -> documentGrid.getStatus().getMessage()))
                .build();
    }

    List<DocumentDTO> toDTO(List<DocumentGrid> documentGrids);

    UserEO toEO(User user);

    @Mappings({
            @Mapping(target = "userId", ignore = true),
            @Mapping(target = "name", ignore = true),
            @Mapping(target = "email", ignore = true),
            @Mapping(target = "active", ignore = true),
            @Mapping(target = "positions", ignore = true),
            @Mapping(target = "properties", ignore = true),
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
