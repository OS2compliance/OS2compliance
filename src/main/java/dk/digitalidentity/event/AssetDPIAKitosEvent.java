package dk.digitalidentity.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDPIAKitosEvent {
    private long assetId;
	private String assetKitosItSystemUsageId;
	private Date dpiaDate;
	private String dpiaUrl;
	private String dpiaName;
}
