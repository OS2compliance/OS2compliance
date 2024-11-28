package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentDTO {
    private Long id;
    private String name;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
    private List<IncidentFieldResponseDTO> responses = new ArrayList<>();
}

