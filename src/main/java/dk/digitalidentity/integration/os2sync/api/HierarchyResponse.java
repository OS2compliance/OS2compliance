package dk.digitalidentity.integration.os2sync.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class HierarchyResponse {
    @JsonProperty("Created")
    private OffsetDateTime created;
    @JsonProperty("Result")
    private HierarchyResult result;
    @JsonProperty("Status")
    private Integer status;
}
