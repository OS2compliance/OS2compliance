package dk.digitalidentity.integration.cvr.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class HentVirksomhedResultat {
    private Virksomhed virksomhed;
    private Virksomhedsnavn virksomhedsnavn;
    private Telefonnummer telefonnummer;
    private Beliggenhedsadresse beliggenhedsadresse;
    @JsonProperty("e-mailadresse")
    private Email email;
    @JsonProperty("produktionsenheder")
    private List<Produktionsenhed> produktionsenheder;

}
