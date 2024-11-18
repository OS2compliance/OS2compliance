package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {
    private Long id;
    private String name;
    private String documentType;
    private Integer documentTypeOrder;
    private String responsibleUser;
    private String nextRevision;
    private String status;
    private Integer statusOrder;
    private String tags;
    private boolean changeable;
}
