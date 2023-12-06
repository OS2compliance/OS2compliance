package dk.digitalidentity.integration.cvr.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Beliggenhedsadresse {
    @JsonProperty("CVRAdresse_husnummerFra")
    private String husnummer;

    @JsonProperty("CVRAdresse_postdistrikt")
    private String postdistrikt;

    @JsonProperty("CVRAdresse_vejnavn")
    private String vejnavn;

    @JsonProperty("CVRAdresse_postnummer")
    private String postnummer;

    @JsonProperty("CVRAdresse_landekode")
    private String landekode;

}
