package dk.digitalidentity.integration.cvr.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CvrSearchResultDTO {
    private String name;
    private String cvr;
    private String address;
    private String zipCode;
    private String city;
    private String country;
    private String phone;
    private String email;
}
