package dk.digitalidentity.mapping;

import dk.digitalidentity.model.api.DocumentCreateEO;
import dk.digitalidentity.model.api.DocumentEO;
import dk.digitalidentity.model.api.PageEO;
import dk.digitalidentity.model.api.UserEO;
import dk.digitalidentity.model.api.UserWriteEO;
import dk.digitalidentity.model.dto.DocumentDTO;
import dk.digitalidentity.model.dto.TagDTO;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.DocumentGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.tag.TagService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DocumentMapper {

	default DocumentDTO toDTO(final DocumentGrid documentGrid, Map<Long, Tag> tagsById) {
		List<TagDTO> tags = TagService.toTagDTO(documentGrid.getTagIds(), tagsById).stream().sorted(Comparator.comparing(TagDTO::getLabel)).toList();

		DocumentDTO documentDTO = DocumentDTO.builder()
				.id(documentGrid.getId())
				.name(documentGrid.getName())
				.documentType(documentGrid.getDocumentType())
				.documentTypeOrder(documentGrid.getDocumentTypeOrder())
				.responsibleUser(nullSafe(() -> documentGrid.getResponsibleUser().getName(), ""))
				.nextRevision(nullSafe(() -> documentGrid.getNextRevision().format(DK_DATE_FORMATTER)))
				.status(nullSafe(() -> documentGrid.getStatus().getMessage()))
				.statusOrder(documentGrid.getStatusOrder())
				.tags(tags)
				.build();

		Set<AllowedAction> allowedActions = new HashSet<>();
		boolean isResponsible = (documentGrid.getResponsibleUser() != null && documentGrid.getResponsibleUser().getUuid().equals(SecurityUtil.getPrincipalUuid()));
		if (SecurityUtil.isOperationAllowed(Roles.DELETE_ALL)
				|| (isResponsible && SecurityUtil.isOperationAllowed(Roles.DELETE_OWNER_ONLY))) {
			allowedActions.add(AllowedAction.DELETE);
		}

		documentDTO.setAllowedActions(allowedActions);
		return documentDTO;

	}

	default List<DocumentDTO> toDTO(List<DocumentGrid> documentGrid, Map<Long, Tag> tagsById) {
		List<DocumentDTO> documentDTOS = new ArrayList<>();
		documentGrid.forEach(a -> documentDTOS.add(toDTO(a, tagsById)));
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
			@Mapping(target = "roles", ignore = true)
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
			@Mapping(target = "localizedEnums", ignore = true),
			@Mapping(target = "documentType", ignore = true),
			@Mapping(target = "includeInYearWheel", ignore = true)
	})
	Document fromEO(DocumentCreateEO documentCreateEO);

	default DocumentEO.DocumentType map(final ChoiceValue choiceValue) {
		if (choiceValue == null) {
			return null;
		}

		return switch (choiceValue.getIdentifier()) {
			case "document-type-other-123456" -> DocumentEO.DocumentType.OTHER;
			case "document-type-workflow-123456" -> DocumentEO.DocumentType.WORKFLOW;
			case "document-type-data-processing-agreement-123456" -> DocumentEO.DocumentType.DATA_PROCESSING_AGREEMENT;
			case "document-type-contract-123456" -> DocumentEO.DocumentType.CONTRACT;
			case "document-type-control-123456" -> DocumentEO.DocumentType.CONTROL;
			case "document-type-management-report-123456" -> DocumentEO.DocumentType.MANAGEMENT_REPORT;
			case "document-type-procedure-123456" -> DocumentEO.DocumentType.PROCEDURE;
			case "document-type-risk-assessment-report-123456" -> DocumentEO.DocumentType.RISK_ASSESSMENT_REPORT;
			case "document-type-supervisory-report-123456" -> DocumentEO.DocumentType.SUPERVISORY_REPORT;
			case "document-type-guide-123456" -> DocumentEO.DocumentType.GUIDE;
			default -> {
				// map to OTHER. This means that every new ChoiceValue documentType will have Other as type in v1 api
				yield DocumentEO.DocumentType.OTHER;
			}
		};
	}

}
