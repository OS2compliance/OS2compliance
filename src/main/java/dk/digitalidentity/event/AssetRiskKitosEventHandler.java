package dk.digitalidentity.event;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.integration.kitos.KitosClientService;
import dk.digitalidentity.integration.kitos.KitosConstants;
import dk.digitalidentity.simple_queue.QueueMessage;
import dk.digitalidentity.simple_queue.SimpleMessageHandler;
import dk.digitalidentity.simple_queue.json.JsonSimpleMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetRiskKitosEventHandler implements SimpleMessageHandler {

	private final KitosClientService clientService;
	private final OS2complianceConfiguration configuration;


	@Override
	public boolean handles(QueueMessage message) {
		return KitosConstants.KITOS_ASSET_RISK_CHANGED_QUEUE.equals(message.getQueue());
	}

	@Override
	public boolean handleMessage(QueueMessage message) {
		final AssetRiskKitosEvent event = JsonSimpleMessage.fromJson(message.getBody(), AssetRiskKitosEvent.class);

		if (configuration.getIntegrations().getKitos().isEnabled()) {
			if (event.getAssetKitosItSystemUsageId() != null) {
				clientService.updateAssetRiskAssessment(event.getAssetKitosItSystemUsageId(), event);
				log.info("Updated riskAssessment for asset: {}", event.getAssetId());
			}
		}

		return true;
	}

	@Override
	public boolean handleFailedMessage(QueueMessage message, Exception exception) {
		log.error("Failed to handle message: {}", message, exception);
		return false;
	}
}
