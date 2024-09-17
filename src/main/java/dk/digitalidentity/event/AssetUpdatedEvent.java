package dk.digitalidentity.event;

import dk.digitalidentity.model.api.AssetEO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetUpdatedEvent {
    private AssetEO asset;
}
