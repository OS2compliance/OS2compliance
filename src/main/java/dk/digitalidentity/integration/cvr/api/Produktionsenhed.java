package dk.digitalidentity.integration.cvr.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Produktionsenhed {
    @JsonProperty("pNummer")
    private String pnr;

    @JsonProperty("produktionsenhedsnavn")
    private Produktionsenhedsnavn navn;

}
