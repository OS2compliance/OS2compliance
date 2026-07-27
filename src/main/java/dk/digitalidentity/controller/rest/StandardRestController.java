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
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireCreateOwnerOnly;
import dk.digitalidentity.security.annotations.crud.RequireUpdateAll;
import dk.digitalidentity.security.annotations.sections.RequireStandard;
import dk.digitalidentity.security.annotations.crud.RequireDeleteOwnerOnly;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.util.StandardSectionNumbering;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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

	/** Afviser absurd store kald foer de rammer findAllById; gruppens faktiske stoerrelse tjekkes bagefter. */
	private static final int MAX_REORDER_SECTIONS = 1000;

	record SetFieldDTO(@NotNull SetFieldStandardType setFieldType, @NotNull String value) {}
	@RequireUpdateAll
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

    record StandardSectionRecord(Long id, String description, Long[] documents, Long[] relations, StandardSectionStatus status){}
	@RequireCreateOwnerOnly
    @Transactional
    @PostMapping(value = "save", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> save(@RequestBody StandardSectionRecord record) {
        final StandardSection section = standardSectionDao.findById(record.id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if(section.getResponsibleUser() != null &&  section.getResponsibleUser().getUuid().equals(SecurityUtil.getPrincipalUuid())) {
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
		// Kravet kan mangle sin StandardSection (bad-state fra tidligere fejl) - slet alligevel
		// template-sektionen, saa brugeren kan komme af med et krav der ellers ikke kan slettes.
		standardSectionDao.findByTemplateSectionIdentifier(identifier).ifPresent(standardSectionDao::delete);
		standardTemplateSectionDao.delete(template);

		StandardTemplateSection parent = template.getParent();
		if (parent == null) {
			return new ResponseEntity<>(HttpStatus.OK);
		}
		renumberInOrder(parent, standardTemplateSectionDao.findByParentOrderBySortKey(parent));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireUpdateAll
	@Transactional
	@PostMapping("{templateIdentifier}/section/reorder")
	public ResponseEntity<?> reorderSections(@PathVariable(name = "templateIdentifier") final String templateIdentifier,
											 @RequestBody final List<String> identifiers) {
		if (identifiers == null || identifiers.isEmpty() || identifiers.size() > MAX_REORDER_SECTIONS) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		// Faerre fundne end sendt = ukendte identifiere eller dubletter; findAllById er distinkt.
		List<StandardTemplateSection> sections = standardTemplateSectionDao.findAllById(identifiers);
		if (sections.size() != identifiers.size()) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		StandardTemplateSection parent = sections.get(0).getParent();
		if (parent == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		// Uden denne kunne et kald omdoebe de indbyggede ISO 27001-punkter, hvis navne bruges i
		// relationer, opgaver, rapporter og global soegning.
		StandardTemplate template = parent.getStandardTemplate();
		if (template == null || !template.getIdentifier().equals(templateIdentifier) || !template.isSupporting()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		// Hele gruppen og kun gruppen: omnummerering af en delmaengde ville efterlade dubletter.
		long siblingCount = standardTemplateSectionDao.findByParentOrderBySortKey(parent).size();
		if (sections.stream().anyMatch(s -> s.getParent() == null || !s.getParent().getIdentifier().equals(parent.getIdentifier()))
				|| identifiers.size() != siblingCount) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		Map<String, StandardTemplateSection> byId = sections.stream()
				.collect(java.util.stream.Collectors.toMap(StandardTemplateSection::getIdentifier, s -> s));
		renumberInOrder(parent, identifiers.stream().map(byId::get).toList());
		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * Nummererer gruppens krav 1, 2, 3 ... i listens raekkefoelge. Skriver ogsaa
	 * StandardSection.name, saa nummeret er det samme i oversigten og de steder navnet bruges -
	 * relationer, opgaver, rapporter og global soegning.
	 */
	private void renumberInOrder(final StandardTemplateSection parent, final List<StandardTemplateSection> ordered) {
		String baseSection = parent.getSection();
		List<StandardTemplateSection> toBeUpdatedstandardTemplateSection = new ArrayList<>();
		List<StandardSection> toBeUpdatedStandardSection = new ArrayList<>();
		int version = 1;
		for (StandardTemplateSection sibling : ordered) {
			String newSectionName = baseSection + "." + version;

			sibling.setSection(newSectionName);
			sibling.setSortKey(StandardSectionNumbering.sortKeyOf(baseSection, version));
			toBeUpdatedstandardTemplateSection.add(sibling);

			// Et krav uden StandardSection (bad-state) nummereres alligevel ovenfor - springes det
			// over, beholder det sit gamle nummer og kolliderer med et af de nyudstedte.
			StandardSection section = sibling.getStandardSection();
			if (section != null) {
				// Bevar titel-teksten efter det gamle nummer-token (null-sikkert).
				String title = section.getName() == null ? "" : section.getName().replaceFirst("^\\S+\\s*", "");
				section.setName(title.isEmpty() ? newSectionName : newSectionName + " " + title);
				toBeUpdatedStandardSection.add(section);
			}

			version++;
		}

		standardSectionDao.saveAll(toBeUpdatedStandardSection);
		standardTemplateSectionDao.saveAll(toBeUpdatedstandardTemplateSection);
	}

	@Transactional
	@PostMapping("/header/delete/{identifier}")
	public ResponseEntity<HttpStatus> deleteHeader(@PathVariable(name = "identifier") final String identifier) {
		StandardTemplateSection section =  standardTemplateSectionDao.findById(identifier).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
		standardTemplateSectionDao.delete(section);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@Transactional
	@PostMapping("/delete/{id}")
	public ResponseEntity<HttpStatus> deleteStandard(@PathVariable(name = "id") final String id) {
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
