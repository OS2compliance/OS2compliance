package dk.digitalidentity.model.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TagDTO {
	private String label;
	private String color;
	private String contrast;
}
