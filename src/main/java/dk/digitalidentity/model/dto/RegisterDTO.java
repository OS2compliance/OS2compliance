package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO {
    private Long id;
    private String name;
    private String packageName;
    private String description;
    private String responsibleUsers;
    private String responsibleOUs;
    private String departments;
    private String updatedAt;
    private String consequence;
    private String risk;
    private String status;
    private Set<String> gdprChoices;
    private int assetCount;
}
