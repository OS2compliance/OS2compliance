package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AssetIdNameDTO {
    private Long id;
    private String name;

    public String getLabel() {
        return name;
    }
}