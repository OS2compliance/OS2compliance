package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.AssetGridDao;
import dk.digitalidentity.mapping.AssetMapper;
import dk.digitalidentity.model.dto.AssetDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetOversight;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.DPIATemplateQuestion;
import dk.digitalidentity.model.entity.DPIATemplateSection;
import dk.digitalidentity.model.entity.DataProtectionImpactAssessmentScreening;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.ChoiceOfSupervisionModel;
import dk.digitalidentity.model.entity.grid.AssetGrid;
import dk.digitalidentity.security.RequireSuperuserOrAdministrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.AssetOversightService;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.DPIAService;
import dk.digitalidentity.service.DPIATemplateQuestionService;
import dk.digitalidentity.service.DPIATemplateSectionService;
import dk.digitalidentity.service.UserService;
import dk.digitalidentity.util.ReflectionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dk.digitalidentity.service.UserService;
import dk.digitalidentity.util.ReflectionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

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
    private final AssetOversightService assetOversightService;
	private final DPIAService dPIAService;

	@PostMapping("list")
    public PageDTO<AssetDTO> list(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "limit", defaultValue = "50") int limit,
        @RequestParam(value = "order", required = false) String sortColumn,
        @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
        @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {
        Page<AssetGrid> assets =  assetGridDao.findAllWithColumnSearch(
            validateSearchFilters(filters, AssetGrid.class),
            null,
            buildPageable(page, limit, sortColumn, sortDirection),
            AssetGrid.class
        );

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new PageDTO<>(assets.getTotalElements(), mapper.toDTO(assets.getContent(),
            authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)), SecurityUtil.getPrincipalUuid()));
    }

    @PostMapping("list/{id}")
    public PageDTO<AssetDTO> list(
        @PathVariable(name = "id") final String uuid,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "limit", defaultValue = "50") int limit,
        @RequestParam(value = "order", required = false) String sortColumn,
        @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
        @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {
        final User user = userService.findByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!SecurityUtil.isSuperUser() && !uuid.equals(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Page<AssetGrid> assets = null;
        assets = assetGridDao.findAllForResponsibleUser(
            validateSearchFilters(filters,AssetGrid.class ),
            buildPageable(page, limit, sortColumn, sortDirection),
            AssetGrid.class,
            user);

        return new PageDTO<>(assets.getTotalElements(), mapper.toDTO(assets.getContent(), authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)), SecurityUtil.getPrincipalUuid()));
    }

    @PutMapping("{id}/setfield")
    public void setAssetField(@PathVariable("id") final Long id, @RequestParam("name") final String fieldName,
                              @RequestParam(value = "value", required = false) final String value) {
        canSetFieldGuard(fieldName);
        final Asset asset = assetService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        ReflectionHelper.callSetterWithParam(Asset.class, asset, fieldName, value);
        assetService.save(asset);
    }

    @PutMapping("{id}/dpiascreening/setfield")
    public void setDpiaScreeningField(@PathVariable("id") final Long id, @RequestParam("name") final String fieldName,
                                      @RequestParam(value = "value", required = false) final String value) {
        canSetFieldDPIAScreeningGuard(fieldName);
		DPIA dpia = dPIAService.find(id);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !isResponsibleForAsset(dpia.getAssets())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        ReflectionHelper.callSetterWithParam(DataProtectionImpactAssessmentScreening.class, dpia.getDpiaScreening(), fieldName, value);
    }

    @Transactional
    @PutMapping("{id}/oversightresponsible")
    public void setOversightResponsible(@PathVariable("id") final Long id, @RequestParam("userUuid") final String userUuid) {
        final Asset asset = assetService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final User user = userService.findByUuid(userUuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        asset.setOversightResponsibleUser(user);
        if (asset.getSupervisoryModel() != ChoiceOfSupervisionModel.DBS) {
            assetOversightService.setAssetsToDbsOversight(Collections.singletonList(asset));
        } else {
            assetOversightService.createOrUpdateAssociatedOversightCheck(asset);
        }
    }

    @Transactional
    @RequireSuperuserOrAdministrator
    @DeleteMapping("{id}/oversightresponsible")
    public void removeOversightResponsibl(@PathVariable("id") final Long id) {
        final Asset asset = assetService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        asset.setOversightResponsibleUser(null);
    }

    record DPIASetFieldDTO(long id, String fieldName, String value) {}
    @RequireSuperuserOrAdministrator
    @PutMapping("dpia/schema/section/setfield")
    public void setDPIASectionField(@RequestBody final DPIASetFieldDTO dto) {
        canSetDPIASectionFieldGuard(dto.fieldName);
        final DPIATemplateSection dpiaTemplateSection = dpiaTemplateSectionService.findById(dto.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ReflectionHelper.callSetterWithParam(DPIATemplateSection.class, dpiaTemplateSection, dto.fieldName, dto.value);
        dpiaTemplateSectionService.save(dpiaTemplateSection);
    }

    @RequireSuperuserOrAdministrator
    @PostMapping("dpia/schema/section/{id}/up")
    public ResponseEntity<?> reorderUp(@PathVariable("id") final long id) {
        reorderSections(id, false);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequireSuperuserOrAdministrator
    @PostMapping("dpia/schema/section/{id}/down")
    public ResponseEntity<?> reorderDown(@PathVariable("id") final long id) {
        reorderSections(id, true);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequireSuperuserOrAdministrator
    @PostMapping("dpia/schema/question/{id}/up")
    public ResponseEntity<?> reorderQuestionUp(@PathVariable("id") final long id) {
        reorderQuestions(id, false);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequireSuperuserOrAdministrator
    @PostMapping("dpia/schema/question/{id}/down")
    public ResponseEntity<?> reorderQuestionDown(@PathVariable("id") final long id) {
        reorderQuestions(id, true);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequireSuperuserOrAdministrator
    @DeleteMapping("dpia/schema/question/{id}/delete")
    public ResponseEntity<?> deleteQuestion(@PathVariable("id") final long id) {
        final DPIATemplateQuestion question = dpiaTemplateQuestionService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        question.setDeleted(true);
        dpiaTemplateQuestionService.save(question);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Transactional
    @DeleteMapping("oversight/{oversightId}")
    public ResponseEntity<?> deleteOversight(@PathVariable("oversightId") Long oversightId) {
        final AssetOversight assetOversight = assetService.getOversight(oversightId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER) && assetOversight.getResponsibleUser().getUuid().equals(SecurityUtil.getPrincipalUuid()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        assetOversightService.delete(assetOversight);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequireSuperuserOrAdministrator
    @Transactional
    @DeleteMapping("{assetId}/subsupplier/{subSupplierId}")
    public ResponseEntity<?> subSupplierDelete(@PathVariable("subSupplierId") final Long subSupplierId,
                                               @PathVariable("assetId") final Long assetId) {
        final Asset asset = assetService.get(assetId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final AssetSupplierMapping subSupplier = asset.getSuppliers().stream().filter(s -> Objects.equals(s.getId(), subSupplierId)).findAny()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        asset.getSuppliers().remove(subSupplier);
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
            fieldName.equals("dpiaOptOutReason") ||
            fieldName.equals("dpiaOptOut"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private void canSetFieldDPIAScreeningGuard(final String fieldName) {
        if (!(fieldName.equals("consequenceLink"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

	private boolean isResponsibleForAsset(List<Asset> assets) {
		return assets.stream().flatMap(a ->
						a.getResponsibleUsers().stream()
								.map(User::getUuid))
				.toList()
				.contains(SecurityUtil.getPrincipalUuid());
	}


}
