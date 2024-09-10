package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InactiveResponsibleDTO {
    private String uuid;
    private String name;
    private String userId;
    private String email;
    private List<RelatableDTO> responsibleFor;
}
