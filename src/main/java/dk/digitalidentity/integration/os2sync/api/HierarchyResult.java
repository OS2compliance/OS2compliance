package dk.digitalidentity.integration.os2sync.api;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HierarchyResult {
    @JsonProperty("OUs")
    private List<HierarchyOU> ous = new ArrayList<>();
    @JsonProperty("Users")
    private List<HierarchyUser> users = new ArrayList<>();
}
