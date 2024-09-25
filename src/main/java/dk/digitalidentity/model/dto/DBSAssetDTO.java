package dk.digitalidentity.model.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import dk.digitalidentity.model.entity.Asset;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DBSAssetDTO {
    private Long id;
    private String name;
    @JsonFormat(pattern="dd/MM-yyyy")
    private LocalDate lastSync;
    private List<Asset> assets;
    private String supplier;
}
