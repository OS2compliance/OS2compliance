package dk.digitalidentity.event;

import dk.digitalidentity.config.GRComplianceConfiguration;
import dk.digitalidentity.integration.kitos.KitosClientService;
import dk.digitalidentity.model.api.AssetEO;
import dk.digitalidentity.model.api.PropertyEO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static dk.digitalidentity.integration.kitos.KitosConstants.KITOS_USAGE_UUID_PROPERTY_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetEventHandler {
    private final KitosClientService clientService;
    private final GRComplianceConfiguration configuration;

    @Async
    @EventListener
    public void assetUpdated(final AssetUpdatedEvent event) {
        log.info("Asset updated event fired");
        final AssetEO asset = event.getAsset();
        if (configuration.getIntegrations().getKitos().isEnabled()) {
            findKitosUsageUuid(asset).ifPresent(uuid -> {
                if (asset.getCriticality() != null) {
                    clientService.updateBusinessCritical(uuid, asset.getCriticality().equals(AssetEO.Criticality.CRITICAL));
                    log.info("Updated criticality for asset: {}", asset.getId());
                }
            });
        }
    }

    private static Optional<String> findKitosUsageUuid(final AssetEO asset) {
        return asset.getProperties().stream()
            .filter(p -> p.getKey().equals(KITOS_USAGE_UUID_PROPERTY_KEY))
            .map(PropertyEO::getValue)
            .findFirst();
    }


}
