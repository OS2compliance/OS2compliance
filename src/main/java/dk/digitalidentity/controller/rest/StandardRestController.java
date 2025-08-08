package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.StandardSectionDao;
import dk.digitalidentity.dao.StandardTemplateDao;
import dk.digitalidentity.dao.StandardTemplateSectionDao;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.dto.enums.SetFieldStandardType;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.StandardSectionStatus;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireCreateOwnerOnly;
import dk.digitalidentity.security.annotations.crud.RequireUpdateOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireStandard;
import dk.digitalidentity.security.annotations.crud.RequireDeleteOwnerOnly;
import dk.digitalidentity.service.RelationService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("rest/standards")
@RequireStandard
@RequiredArgsConstructor
public class StandardRestController {
    private final StandardSectionDao standardSectionDao;
    private final UserDao userDao;
    private final RelationService relationService;
	private final StandardTemplateSectionDao standardTemplateSectionDao;
	private final StandardTemplateDao standardTemplateDao;

	record SetFieldDTO(@NotNull SetFieldStandardType setFieldType, @NotNull String value) {}
	@RequireUpdateOwnerOnly
    @PostMapping("{templateIdentifier}/supporting/standardsection/{id}")
    public ResponseEntity<HttpStatus> setField(@PathVariable final String templateIdentifier, @PathVariable final long id, @Valid @RequestBody final SetFieldDTO dto) {
        final StandardSection standardSection = standardSectionDao.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPER_USER)) && standardSection.getResponsibleUser().getUuid().equals(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

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
    record StandardSectionRecord(Long id, String description, Long[] documents, Long[] relations, StandardSectionStatus status){}

	@RequireCreateOwnerOnly
    @Transactional
    @PostMapping(value = "save", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> save(@RequestBody StandardSectionRecord record) {
        final StandardSection section = standardSectionDao.findById(record.id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPER_USER)) && section.getResponsibleUser().getUuid().equals(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        final HashSet<Long> combinedRelations = new HashSet<>();
        if (record.documents != null) {
            combinedRelations.addAll(List.of(record.documents));
        }
        if (record.relations != null) {
            combinedRelations.addAll(List.of(record.relations));
        }
        relationService.setRelationsAbsolute(section, combinedRelations);
        section.setStatus(record.status);
        section.setDescription(record.description);

        return new ResponseEntity<>(HttpStatus.OK);
    }

	@RequireDeleteOwnerOnly
	@Transactional
	@PostMapping("/section/delete/{identifier}")
	public ResponseEntity<?> deleteSection(@PathVariable(name = "identifier") final String identifier) {
		StandardTemplateSection template = standardTemplateSectionDao.findById(identifier).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
		StandardSection relatedSection = standardSectionDao.findByTemplateSectionIdentifier(identifier).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
		standardSectionDao.delete(relatedSection);
		standardTemplateSectionDao.delete(template);

		StandardTemplateSection parent = template.getParent();
		List<StandardTemplateSection> siblings = standardTemplateSectionDao.findByParentOrderBySortKey(parent);
		List<StandardTemplateSection> toBeUpdatedstandardTemplateSection = new ArrayList<>();
		List<StandardSection> toBeUpdatedStandardSection = new ArrayList<>();
		int version = 1;
		for (StandardTemplateSection sibling : siblings) {
			StandardSection section = sibling.getStandardSection();
			if (section == null) {
				continue;
			}

			String baseSection = parent.getSection();
			String newSectionName = baseSection + "." + version;

			section.setName(newSectionName + section.getName().replaceFirst("^[^.]+", ""));
			sibling.setSortKey(Integer.parseInt(baseSection.replace(".", "") + version));

			sibling.setSection(newSectionName);
			toBeUpdatedstandardTemplateSection.add(sibling);
			toBeUpdatedStandardSection.add(section);

			version++;
		}

		standardSectionDao.saveAll(toBeUpdatedStandardSection);
		standardTemplateSectionDao.saveAll(toBeUpdatedstandardTemplateSection);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@Transactional
	@PostMapping("/header/delete/{identifier}")
	public ResponseEntity<?> deleteHeader(@PathVariable(name = "identifier") final String identifier) {
		StandardTemplateSection section =  standardTemplateSectionDao.findById(identifier).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
		standardTemplateSectionDao.delete(section);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@Transactional
	@PostMapping("/delete/{id}")
	public ResponseEntity<?> deleteStandard(@PathVariable(name = "id") final String id) {
		StandardTemplate template = standardTemplateDao.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
		standardTemplateDao.delete(template);
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
