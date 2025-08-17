package dk.digitalidentity.service.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.digitalidentity.mapping.RegisterMapper;
import dk.digitalidentity.model.dto.RegisterDTO;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.kle.KLEGroup;
import dk.digitalidentity.model.entity.kle.KLEMainGroup;
import dk.digitalidentity.service.RegisterService;
import dk.digitalidentity.service.kle.KLEGroupService;
import dk.digitalidentity.service.kle.KLEMainGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

	@Transactional
    public void importRegister(final Resource resource) throws IOException {
        final String jsonString = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        final RegisterDTO registerDTO = objectMapper.readValue(jsonString, RegisterDTO.class);

		final Register saved = registerService.findByName(registerDTO.getName())
				.orElseGet(() -> registerService.save(registerMapper.fromDTO(registerDTO)));
		if (saved.getKleMainGroups().isEmpty() && saved.getKleGroups().isEmpty()) {
			final Set<KLEMainGroup> mainGroups = kleMainGroupService.getAllByMainGroupNumbers(registerDTO.getKleGroups());
			final Set<KLEGroup> groups = kleGroupService.getAllByGroupNumbers(registerDTO.getKleGroups());
			saved.setKleMainGroups(mainGroups);
			saved.setKleGroups(groups);
		}
    }

    @Transactional
    public void updateRegisterGdprChoices(final Resource resource) throws IOException {
        final String jsonString = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        final RegisterDTO registerDTO = objectMapper.readValue(jsonString, RegisterDTO.class);
        registerService.findByName(registerDTO.getName())
            .ifPresent(register -> register.setGdprChoices(registerDTO.getGdprChoices()));
    }
}
