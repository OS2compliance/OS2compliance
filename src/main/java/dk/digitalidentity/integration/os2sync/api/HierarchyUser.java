package dk.digitalidentity.integration.os2sync.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Data
@ToString
public class HierarchyUser {
    @JsonProperty("Uuid")
    private String uuid;
    @JsonProperty("UserId")
    private String userId;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Email")
    private String email;
    @JsonProperty("Telephone")
    private String telephone;
    @JsonProperty("Positions")
    private Set<HierarchyPosition> positions = new HashSet<>();
}
