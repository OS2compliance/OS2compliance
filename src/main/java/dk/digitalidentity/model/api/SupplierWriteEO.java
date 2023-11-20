package dk.digitalidentity.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static dk.digitalidentity.model.api.Examples.ID_EXAMPLE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "SupplierWrite")
public class SupplierWriteEO {
    @Schema(description = "ID in OS2compliance", accessMode = Schema.AccessMode.READ_ONLY, example = ID_EXAMPLE)
    private Long id;
}
