package dk.digitalidentity.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@EqualsAndHashCode
public class KLELegalReferenceDTO {
	private String title;
	private String paragraph;
	private String value;
	private String url;
	private boolean selected;
}
