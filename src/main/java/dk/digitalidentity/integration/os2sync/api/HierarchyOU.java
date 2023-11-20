package dk.digitalidentity.integration.os2sync.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class HierarchyOU {
    @JsonProperty("Uuid")
    private String uuid;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("ParentOU")
    private String parentUUID;
}
