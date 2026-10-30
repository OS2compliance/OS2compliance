package dk.digitalidentity.service;

import dk.digitalidentity.model.entity.ChoiceValue;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ChoiceServiceTest {

	@Test
	void gdprOrderPlacesArticle10AfterArticle9AndBeforeDatabeskyttelsesloven() {
		final var sorted = Stream.of("valp10", "valp8", "art10", "valp7", "valp6", "valp9")
				.map(id -> ChoiceValue.builder().identifier("register-gdpr-" + id).build())
				.sorted(ChoiceService.GDPR_ORDER)
				.map(ChoiceValue::getIdentifier)
				.toList();

		assertThat(sorted).containsExactly("register-gdpr-valp6", "register-gdpr-valp7", "register-gdpr-art10",
				"register-gdpr-valp8", "register-gdpr-valp9", "register-gdpr-valp10");
	}
}
