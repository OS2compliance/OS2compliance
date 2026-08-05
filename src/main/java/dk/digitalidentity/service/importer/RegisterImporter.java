package dk.digitalidentity.service.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.digitalidentity.mapping.RegisterMapper;
import dk.digitalidentity.model.dto.RegisterDTO;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.kle.KLEGroup;
import dk.digitalidentity.model.entity.kle.KLEMainGroup;
import dk.digitalidentity.model.entity.kle.KLESubject;
import dk.digitalidentity.service.ChoiceValueService;
import dk.digitalidentity.service.RegisterService;
import dk.digitalidentity.service.kle.KLEGroupService;
import dk.digitalidentity.service.kle.KLEMainGroupService;
import dk.digitalidentity.service.kle.KLESubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegisterImporter {
    private final ObjectMapper objectMapper;
    private final RegisterService registerService;
    private final RegisterMapper registerMapper;
	private final KLEMainGroupService kleMainGroupService;
	private final KLEGroupService kleGroupService;
	private final KLESubjectService kleSubjectService;
	private final ChoiceValueService choiceValueService;

	@Transactional
    public void importRegister(final Resource resource) throws IOException {
        final String jsonString = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        final RegisterDTO registerDTO = objectMapper.readValue(jsonString, RegisterDTO.class);

		final Register saved = registerService.findByName(registerDTO.getName())
				.orElseGet(() -> registerService.save(registerMapper.fromDTO(registerDTO, choiceValueService)));
		if (saved.getKleMainGroups().isEmpty() || saved.getKleGroups().isEmpty()) {
			final Set<KLEMainGroup> mainGroups = kleMainGroupService.getAllByMainGroupNumbers(registerDTO.getKleGroups());
			final Set<KLEGroup> groups = kleGroupService.getAllByGroupNumbers(registerDTO.getKleGroups());
			saved.setKleMainGroups(mainGroups);
			saved.setKleGroups(groups);
		}
    }

	@Transactional
	public void enrichWithKLE(final Resource resource) throws IOException {
		final String jsonString = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		final RegisterDTO registerDTO = objectMapper.readValue(jsonString, RegisterDTO.class);

		registerService.findByName(registerDTO.getName()).ifPresent(register -> {
			final Set<KLEMainGroup> mainGroups = kleMainGroupService.getAllByMainGroupNumbers(registerDTO.getKleMainGroups());
			final Set<KLEGroup> groups = kleGroupService.getAllByGroupNumbers(registerDTO.getKleGroups());
			final Set<KLESubject> subjects = kleSubjectService.findAllBySubjectNumbers(registerDTO.getKleSubjects());
			register.setKleMainGroups(mainGroups);
			register.setKleGroups(groups);
			register.setKleSubjects(subjects);
		});

	}

	/**
	 * Adds the KLE codes from the package that the register is missing, and removes nothing.
	 * <p>
	 * {@link #enrichWithKLE} can only associate codes that exist in the KLE tables at the moment it
	 * runs, and it runs once, at bootstrap, right after the bundled {@code data/kle-emneplan.xml} has
	 * been loaded. A package referring to a code that KLE published after that snapshot was taken
	 * therefore loses it silently - the lookup simply finds nothing. The nightly {@code KLEApiTask}
	 * adds the code to the database later the same day, but nothing revisits the association.
	 * <p>
	 * This fills that gap after a KLE sync has brought in new codes. It is deliberately additive: a
	 * municipality may have adjusted the KLE on a KL register itself, and repairing our own omission
	 * must not undo their work. Removing a code that KL dropped from the mapping belongs to the
	 * package update in {@code DataBootstrap}, not here.
	 *
	 * @return the number of codes added across all three levels
	 */
	@Transactional
	public int backfillMissingKLE(final Resource resource) throws IOException {
		final String jsonString = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		final RegisterDTO registerDTO = objectMapper.readValue(jsonString, RegisterDTO.class);

		return registerService.findByName(registerDTO.getName()).map(register -> {
			final Set<KLEMainGroup> mainGroups = kleMainGroupService.getAllByMainGroupNumbers(registerDTO.getKleMainGroups());
			final Set<KLEGroup> groups = kleGroupService.getAllByGroupNumbers(registerDTO.getKleGroups());
			final Set<KLESubject> subjects = kleSubjectService.findAllBySubjectNumbers(registerDTO.getKleSubjects());
			int added = 0;
			for (final KLEMainGroup mainGroup : mainGroups) {
				added += register.getKleMainGroups().add(mainGroup) ? 1 : 0;
			}
			for (final KLEGroup group : groups) {
				added += register.getKleGroups().add(group) ? 1 : 0;
			}
			for (final KLESubject subject : subjects) {
				added += register.getKleSubjects().add(subject) ? 1 : 0;
			}
			if (added > 0) {
				log.info("Backfilled {} KLE code(s) on register '{}'", added, register.getName());
			}
			return added;
		}).orElse(0);
	}

    @Transactional
    public void updateRegisterGdprChoices(final Resource resource) throws IOException {
        final String jsonString = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        final RegisterDTO registerDTO = objectMapper.readValue(jsonString, RegisterDTO.class);
        registerService.findByName(registerDTO.getName())
            .ifPresent(register -> register.setGdprChoices(registerDTO.getGdprChoices()));
    }
}
