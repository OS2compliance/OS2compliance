package dk.digitalidentity.mapping;

import dk.digitalidentity.model.api.DocumentCreateEOV2;
import dk.digitalidentity.model.api.DocumentEOV2;
import dk.digitalidentity.model.api.PageEO;
import dk.digitalidentity.model.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DocumentV2Mapper {

	@Mapping(target = "documentTypeIdentifier", source = "documentType.identifier")
	DocumentEOV2 toEO(Document document);

	List<DocumentEOV2> toEO(List<Document> documents);

	default PageEO<DocumentEOV2> toEO(final Page<Document> page) {
		return PageEO.<DocumentEOV2>builder()
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
			@Mapping(target = "responsibleUser", ignore = true),
			@Mapping(target = "includeInYearWheel", ignore = true),
			@Mapping(target = "documentType", ignore = true)
	})
	Document fromEO(DocumentCreateEOV2 documentCreateV2EO);
}