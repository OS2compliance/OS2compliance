package dk.digitalidentity.integration.os2sync.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HierarchyPosition {
    @JsonProperty("Uuid")
    private String ouUuid;
    @JsonProperty("Name")
    private String name;
}
