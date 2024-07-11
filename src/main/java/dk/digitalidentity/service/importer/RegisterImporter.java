package dk.digitalidentity.service.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.digitalidentity.mapping.RegisterMapper;
import dk.digitalidentity.model.dto.RegisterDTO;
import dk.digitalidentity.service.RegisterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class RegisterImporter {
    private final ObjectMapper objectMapper;
    private final RegisterService registerService;
    private final RegisterMapper registerMapper;

    public RegisterImporter(final ObjectMapper objectMapper, final RegisterService registerService, final RegisterMapper registerMapper) {
        this.objectMapper = objectMapper;
        this.registerService = registerService;
        this.registerMapper = registerMapper;
    }

    public void importRegister(final Resource resource) throws IOException {
        final String jsonString = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        final RegisterDTO registerDTO = objectMapper.readValue(jsonString, RegisterDTO.class);
        if (!registerService.existByName(registerDTO.getName())) {
            registerService.save(registerMapper.fromDTO(registerDTO));
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
