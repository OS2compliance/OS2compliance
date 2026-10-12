package dk.digitalidentity.bootstrap;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.service.SettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static dk.digitalidentity.Constants.DATA_MIGRATION_VERSION_SETTING;
import static org.assertj.core.api.Assertions.assertThat;

class DataBootstrapTest extends BaseIntegrationTest {
	@Autowired
	private DataBootstrap dataBootstrap;

	@Autowired
	private SettingsService settingsService;

	@AfterEach
	void cleanup() {
		settingsService.deleteSetting(DATA_MIGRATION_VERSION_SETTING);
	}

	@Test
	void concurrentInstancesRunSeedOnce() {
		settingsService.setInt(DATA_MIGRATION_VERSION_SETTING, 5);
		final AtomicInteger runs = new AtomicInteger();
		final Runnable slowSeed = () -> {
			runs.incrementAndGet();
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		};

		CompletableFuture.allOf(
				CompletableFuture.runAsync(() -> dataBootstrap.incrementAndPerformIfVersion(5, slowSeed)),
				CompletableFuture.runAsync(() -> dataBootstrap.incrementAndPerformIfVersion(5, slowSeed))
		).join();

		assertThat(runs.get()).isEqualTo(1);
	}
}
