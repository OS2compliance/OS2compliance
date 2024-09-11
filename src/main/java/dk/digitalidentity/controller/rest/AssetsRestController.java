package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.AssetGridDao;
import dk.digitalidentity.mapping.AssetMapper;
import dk.digitalidentity.model.dto.AssetDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DPIATemplateQuestion;
import dk.digitalidentity.model.entity.DPIATemplateSection;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.AssetGrid;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.DPIATemplateQuestionService;
import dk.digitalidentity.service.DPIATemplateSectionService;
import dk.digitalidentity.service.UserService;
import dk.digitalidentity.util.ReflectionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("rest/assets")
@RequireUser
@RequiredArgsConstructor
public class AssetsRestController {
    private final AssetService assetService;
	private final AssetGridDao assetGridDao;
	private final AssetMapper mapper;
    private final UserService userService;
    private final DPIATemplateQuestionService dpiaTemplateQuestionService;
    private final DPIATemplateSectionService dpiaTemplateSectionService;


	@PostMapping("list")
	public PageDTO<AssetDTO> list(@RequestParam(name = "search", required = false) final String search,
                                  @RequestParam(name = "page", required = false) final Integer page,
                                  @RequestParam(name = "size", required = false) final Integer size,
                                  @RequestParam(name = "order", required = false) final String order,
                                  @RequestParam(name = "dir", required = false) final String dir) {
		Sort sort = null;
		if (StringUtils.isNotEmpty(order) && containsField(order)) {
			final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
			sort = Sort.by(direction, order);
		} else {
            sort = Sort.by(Sort.Direction.ASC, "name");
        }
		final Pageable sortAndPage = PageRequest.of(page, size, sort);
		Page<AssetGrid> assets = null;
		if (StringUtils.isNotEmpty(search)) {
			final List<String> searchableProperties = Arrays.asList("name", "supplier", "responsibleUserNames", "updatedAt", "localizedEnums");
			// search and page
			assets = assetGridDao.findAllCustom(searchableProperties, search, sortAndPage, AssetGrid.class);
		} else {
			// Fetch paged and sorted
			assets = assetGridDao.findAll(sortAndPage);
		}

		return new PageDTO<>(assets.getTotalElements(), mapper.toDTO(assets.getContent()));
	}
    @PostMapping("list/{id}")
    public PageDTO<AssetDTO> list(@PathVariable(name = "id", required = true) final String uuid,
                                @RequestParam(name = "search", required = false) final String search,
                                @RequestParam(name = "page", required = false) final Integer page,
                                @RequestParam(name = "size", required = false) final Integer size,
                                @RequestParam(name = "order", required = false) final String order,
                                @RequestParam(name = "dir", required = false) final String dir) {
        Sort sort = null;
        final User user = userService.findByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (StringUtils.isNotEmpty(order) && containsField(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        } else {
            sort = Sort.by(Sort.Direction.ASC, "name");
        }
        final Pageable sortAndPage = PageRequest.of(page, size, sort);
        Page<AssetGrid> assets = null;
        if (StringUtils.isNotEmpty(search)) {
            final List<String> searchableProperties = Arrays.asList("name", "supplier", "responsibleUserNames", "updatedAt", "localizedEnums");
            // search and page
            assets = assetGridDao.findAllForResponsibleUser(searchableProperties, search, sortAndPage, AssetGrid.class, user);
        } else {
            // Fetch paged and sorted
            assets = assetGridDao.findAllByResponsibleUserUuidsContaining(user.getUuid(), sortAndPage);
        }

        return new PageDTO<>(assets.getTotalElements(), mapper.toDTO(assets.getContent()));
    }

    @PutMapping("{id}/setfield")
    public void setRiskAssessmentOptOut(@PathVariable("id") final Long id, @RequestParam("name") final String fieldName,
                                        @RequestParam(value = "value", required = false) final String value) {
        canSetFieldGuard(fieldName);
        final Asset asset = assetService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ReflectionHelper.callSetterWithParam(Asset.class, asset, fieldName, value);
        assetService.save(asset);
    }

    record DPIASetFieldDTO(long id, String fieldName, String value) {}
    @PutMapping("dpia/schema/section/setfield")
    public void setDPIASectionField( @RequestBody final DPIASetFieldDTO dto) {
        canSetDPIASectionFieldGuard(dto.fieldName);
        final DPIATemplateSection dpiaTemplateSection = dpiaTemplateSectionService.findById(dto.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ReflectionHelper.callSetterWithParam(DPIATemplateSection.class, dpiaTemplateSection, dto.fieldName, dto.value);
        dpiaTemplateSectionService.save(dpiaTemplateSection);
    }

    @PostMapping("dpia/schema/section/{id}/up")
    public ResponseEntity<?> reorderUp(@PathVariable("id") final long id) {
        reorderSections(id, false);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("dpia/schema/section/{id}/down")
    public ResponseEntity<?> reorderDown(@PathVariable("id") final long id) {
        reorderSections(id, true);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("dpia/schema/question/{id}/up")
    public ResponseEntity<?> reorderQuestionUp(@PathVariable("id") final long id) {
        reorderQuestions(id, false);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("dpia/schema/question/{id}/down")
    public ResponseEntity<?> reorderQuestionDown(@PathVariable("id") final long id) {
        reorderQuestions(id, true);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("dpia/schema/question/{id}/delete")
    public ResponseEntity<?> deleteQuestion(@PathVariable("id") final long id) {
        final DPIATemplateQuestion question = dpiaTemplateQuestionService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        question.setDeleted(true);
        dpiaTemplateQuestionService.save(question);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void reorderQuestions(final long id, final boolean backwards) {
        final DPIATemplateQuestion question = dpiaTemplateQuestionService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final List<DPIATemplateQuestion> allQuestionsInSection = dpiaTemplateQuestionService.findAll().stream()
            .filter(q -> !q.isDeleted() && q.getDpiaTemplateSection().getId() == question.getDpiaTemplateSection().getId())
            .sorted(sortDPIATemplateQuestionComparator(backwards))
            .toList();
        if (!allQuestionsInSection.isEmpty()) {
            DPIATemplateQuestion last = null;
            for (final DPIATemplateQuestion currentQuestion : allQuestionsInSection) {
                if (last != null && currentQuestion.getId() == id) {
                    final Long newKey = last.getSortKey();
                    last.setSortKey(currentQuestion.getSortKey());
                    currentQuestion.setSortKey(newKey);
                    dpiaTemplateQuestionService.save(last);
                    dpiaTemplateQuestionService.save(currentQuestion);
                    break;
                }
                last = currentQuestion;
            }
        }

    }

    private static Comparator<DPIATemplateQuestion> sortDPIATemplateQuestionComparator(final boolean backwards) {
        final Comparator<DPIATemplateQuestion> comparator = Comparator.comparing(DPIATemplateQuestion::getSortKey);
        return backwards ? comparator.reversed() : comparator;
    }

    private void reorderSections(final long id, final boolean backwards) {
        final List<DPIATemplateSection> allSections = dpiaTemplateSectionService.findAll().stream()
            .sorted(sortDPIATemplateSectionComparator(backwards))
            .toList();
        if (!allSections.isEmpty()) {
            DPIATemplateSection last = null;
            for (final DPIATemplateSection currentSection : allSections) {
                if (last != null && currentSection.getId() == id) {
                    final Long newKey = last.getSortKey();
                    last.setSortKey(currentSection.getSortKey());
                    currentSection.setSortKey(newKey);
                    dpiaTemplateSectionService.save(last);
                    dpiaTemplateSectionService.save(currentSection);
                    break;
                }
                last = currentSection;
            }
        }

    }

    private static Comparator<DPIATemplateSection> sortDPIATemplateSectionComparator(final boolean backwards) {
        final Comparator<DPIATemplateSection> comparator = Comparator.comparing(DPIATemplateSection::getSortKey);
        return backwards ? comparator.reversed() : comparator;
    }

    private void canSetDPIASectionFieldGuard(final String fieldName) {
        if (!(fieldName.equals("hasOptedOut"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private void canSetFieldGuard(final String fieldName) {
        if (!(fieldName.equals("threatAssessmentOptOut") ||
            fieldName.equals("threatAssessmentOptOutReason") ||
            fieldName.equals("dpiaOptOut"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

	private boolean containsField(final String fieldName) {
		return fieldName.equals("assessmentOrder")
				|| fieldName.equals("supplier")
				|| fieldName.equals("risk")
				|| fieldName.equals("name")
				|| fieldName.equals("assetType")
				|| fieldName.equals("responsibleUserNames")
				|| fieldName.equals("registers")
				|| fieldName.equals("updatedAt")
				|| fieldName.equals("criticality")
				|| fieldName.equals("assetStatusOrder")
                || fieldName.equals("hasThirdCountryTransfer");
	}
}
