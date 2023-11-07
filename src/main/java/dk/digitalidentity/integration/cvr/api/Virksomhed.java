package dk.digitalidentity.integration.cvr.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Virksomhed {
    @JsonProperty("CVRNummer")
    private String cvrNummer;
    private Status status;
}
