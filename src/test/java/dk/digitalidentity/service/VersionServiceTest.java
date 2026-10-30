package dk.digitalidentity.service;

import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.info.GitProperties;

import static org.assertj.core.api.Assertions.assertThat;

class VersionServiceTest {

	@Test
	void versionInfoOmitsCommitWhenBuiltWithoutGitDirectory() {
		GitProperties git = new GitProperties(new Properties());
		VersionService service = new VersionService(new StaticListableBeanFactory(Map.of("git", git)).getBeanProvider(GitProperties.class));

		assertThat(service.getVersionInfo()).isEqualTo("Bygget: ukendt");
	}
}
