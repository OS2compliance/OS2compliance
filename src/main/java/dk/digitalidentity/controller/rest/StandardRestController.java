package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.StandardSectionDao;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.dto.enums.SetFieldStandardType;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.StandardSectionStatus;
import dk.digitalidentity.security.RequireUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("rest/standards")
@RequireUser
@RequiredArgsConstructor
public class StandardRestController {
    private final StandardSectionDao standardSectionDao;
    private final UserDao userDao;

    record SetFieldDTO(@NotNull SetFieldStandardType setFieldType, @NotNull String value) {}
    @PostMapping("{templateIdentifier}/supporting/standardsection/{id}")
    public ResponseEntity<HttpStatus> setField(@PathVariable final String templateIdentifier, @PathVariable final long id, @Valid @RequestBody final SetFieldDTO dto) {
        final StandardSection standardSection = standardSectionDao.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        switch (dto.setFieldType()) {
            case RESPONSIBLE -> handleResponsibleUser(standardSection, dto.value());
            case STATUS -> standardSection.setStatus(StandardSectionStatus.valueOf(dto.value()));
            case REASON -> standardSection.setReason(dto.value());
            case DESCRIPTION -> standardSection.setDescription(dto.value());
            case SELECTED -> standardSection.setSelected(Boolean.parseBoolean(dto.value()));
            case NSIS_PRACTICE -> standardSection.setNsisPractice(dto.value());
            case NSIS_SMART -> standardSection.setNsisSmart(dto.value());
        }

        standardSectionDao.save(standardSection);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void handleResponsibleUser(final StandardSection standardSection, final String value) {
        if(value.isEmpty()) {
            standardSection.setResponsibleUser(null);
            return;
        }
        final User user = userDao.findById(value).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        standardSection.setResponsibleUser(user);
    }
}
