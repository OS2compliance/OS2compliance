package dk.digitalidentity.mapping;

import dk.digitalidentity.model.KLELegalReferenceDTO;
import dk.digitalidentity.model.entity.kle.KLELegalReference;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KLEMapper {

	default KLELegalReferenceDTO toDTO(final KLELegalReference kleLegalReference) {
		return KLELegalReferenceDTO.builder()
				.title(kleLegalReference.getTitle())
				.url(kleLegalReference.getUrl())
				.paragraph(kleLegalReference.getParagraph())
				.value(kleLegalReference.getAccessionNumber())
				.build();
	}

}
