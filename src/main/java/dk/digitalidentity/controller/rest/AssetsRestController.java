package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.AssetGridDao;
import dk.digitalidentity.event.AssetDPIAKitosEvent;
import dk.digitalidentity.integration.kitos.KitosConstants;
import dk.digitalidentity.mapping.AssetMapper;
import dk.digitalidentity.model.dto.AssetDTO;
import dk.digitalidentity.model.dto.DBSAssetDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetOversight;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.DPIATemplateQuestion;
import dk.digitalidentity.model.entity.DPIATemplateSection;
import dk.digitalidentity.model.entity.DataProtectionImpactAssessmentScreening;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.ChoiceOfSupervisionModel;
import dk.digitalidentity.model.entity.grid.AssetGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireCreateAll;
import dk.digitalidentity.security.annotations.crud.RequireDeleteAll;
import dk.digitalidentity.security.annotations.crud.RequireDeleteOwnerOnly;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.crud.RequireUpdateAll;
import dk.digitalidentity.security.annotations.crud.RequireUpdateOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireAsset;
import dk.digitalidentity.service.AssetOversightService;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.DPIAService;
import dk.digitalidentity.service.DPIATemplateQuestionService;
import dk.digitalidentity.service.DPIATemplateSectionService;
import dk.digitalidentity.service.ExcelExportService;
import dk.digitalidentity.service.UserService;
import dk.digitalidentity.simple_queue.QueueMessage;
import dk.digitalidentity.simple_queue.json.JsonSimpleMessage;
import dk.digitalidentity.util.ReflectionHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static dk.digitalidentity.integration.kitos.KitosConstants.*;
import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Slf4j
@RestController
@RequestMapping("rest/assets")
@RequireAsset
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
	private final ApplicationEventPublisher eventPublisher;
	private final ExcelExportService excelExportService;

	@RequireReadOwnerOnly
	@PostMapping("list")
    public PageDTO<AssetDTO> list(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "limit", defaultValue = "50") int limit,
        @RequestParam(value = "order", required = false) String sortColumn,
        @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
		@RequestParam(value = "export", defaultValue = "false") boolean export,
		@RequestParam(value = "fileName", defaultValue = "export.xlsx") String fileName,
        @RequestParam Map<String, String> filters, // Dynamic filters for search fields
			HttpServletResponse response
    ) throws IOException {

		final String userUuid = SecurityUtil.getLoggedInUserUuid();
		final User user = userService.findByUuid(userUuid)
				.orElseThrow();
		if (userUuid == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		// For export mode, get ALL records (no pagination)
		if (export) {
			Page<AssetGrid> allAssets;
			if (SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
				allAssets = assetGridDao.findAllWithColumnSearch(
						validateSearchFilters(filters, AssetGrid.class),
						buildPageable(page, Integer.MAX_VALUE, sortColumn, sortDirection),
					AssetGrid.class
			);

			} else {
				// Logged in user can see only own
				allAssets = assetGridDao.findAllWithAssignedUser(
						validateSearchFilters(filters, AssetGrid.class),
						user,
						buildPageable(page, Integer.MAX_VALUE, sortColumn, sortDirection),
						AssetGrid.class
				);
			}
				List<AssetDTO> allData = mapper.toDTO(allAssets.getContent());
				excelExportService.exportToExcel(allData, fileName, response);
				return null;
		}

		Page<AssetGrid> assets = null;
		if (SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
			// Logged in user can see all
			assets = assetGridDao.findAllWithColumnSearch(
					validateSearchFilters(filters, AssetGrid.class),
					buildPageable(page, limit, sortColumn, sortDirection),
					AssetGrid.class
			);
		}
		else {
			// Logged in user can see only own
			assets = assetGridDao.findAllWithAssignedUser(
					validateSearchFilters(filters, AssetGrid.class),
					user,
					buildPageable(page, limit, sortColumn, sortDirection),
					AssetGrid.class
			);
		}

		return new PageDTO<>(assets.getTotalElements(), mapper.toDTO(assets.getContent()));
    }

	@RequireReadOwnerOnly
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

        if ( !uuid.equals(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

		Page<AssetGrid> assets = assetGridDao.findAllWithAssignedUser(
				validateSearchFilters(filters, AssetGrid.class),
				user,
				buildPageable(page, limit, sortColumn, sortDirection),
				AssetGrid.class
		);

        return new PageDTO<>(assets.getTotalElements(), mapper.toDTO(assets.getContent()));
    }

	@RequireUpdateOwnerOnly
    @PutMapping("{id}/setfield")
    public void setAssetField(@PathVariable("id") final Long id, @RequestParam("name") final String fieldName,
                              @RequestParam(value = "value", required = false) final String value) {
        canSetFieldGuard(fieldName);
        final Asset asset = assetService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!assetService.isEditable(asset)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        ReflectionHelper.callSetterWithParam(Asset.class, asset, fieldName, value);
        assetService.save(asset);
    }

	@RequireUpdateOwnerOnly
    @PutMapping("{id}/dpiascreening/setfield")
    public void setDpiaScreeningField(@PathVariable("id") final Long id, @RequestParam("name") final String fieldName,
                                      @RequestParam(value = "value", required = false) final String value) {
        canSetFieldDPIAScreeningGuard(fieldName);
		DPIA dpia = dPIAService.find(id);

        if (!SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL) && !isResponsibleForAsset(dpia.getAssets())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        ReflectionHelper.callSetterWithParam(DataProtectionImpactAssessmentScreening.class, dpia.getDpiaScreening(), fieldName, value);
    }

	@RequireUpdateOwnerOnly
    @Transactional
    @PutMapping("{id}/oversightresponsible")
    public void setOversightResponsible(@PathVariable("id") final Long id, @RequestParam("userUuid") final String userUuid) {
        final Asset asset = assetService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final User user = userService.findByUuid(userUuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		if (!SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL) && !assetService.isResponsibleFor(asset)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

        asset.setOversightResponsibleUser(user);
        if (asset.getSupervisoryModel() != ChoiceOfSupervisionModel.DBS) {
            assetOversightService.setAssetsToDbsOversight(Collections.singletonList(asset));
        } else {
            assetOversightService.createOrUpdateAssociatedOversightCheck(asset);
        }
    }

    @Transactional
   	@RequireDeleteOwnerOnly
    @DeleteMapping("{id}/oversightresponsible")
    public void removeOversightResponsibl(@PathVariable("id") final Long id) {
        final Asset asset = assetService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		if (!SecurityUtil.isOperationAllowed(Roles.DELETE_ALL) && !assetService.isResponsibleFor(asset)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

        asset.setOversightResponsibleUser(null);
    }

    public record DPIASetFieldDTO(long id, String fieldName, String value) {}
    @RequireUpdateAll
    @PutMapping("dpia/schema/section/setfield")
    public void setDPIASectionField(@RequestBody final DPIASetFieldDTO dto) {
        canSetDPIASectionFieldGuard(dto.fieldName);
        final DPIATemplateSection dpiaTemplateSection = dpiaTemplateSectionService.findById(dto.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ReflectionHelper.callSetterWithParam(DPIATemplateSection.class, dpiaTemplateSection, dto.fieldName, dto.value);
        dpiaTemplateSectionService.save(dpiaTemplateSection);
    }

    @RequireUpdateAll
    @PostMapping("dpia/schema/section/{id}/up")
    public ResponseEntity<HttpStatus> reorderUp(@PathVariable("id") final long id) {
        reorderSections(id, false);
        return new ResponseEntity<>(HttpStatus.OK);
    }

	@RequireUpdateAll
    @PostMapping("dpia/schema/section/{id}/down")
    public ResponseEntity<HttpStatus> reorderDown(@PathVariable("id") final long id) {
        reorderSections(id, true);
        return new ResponseEntity<>(HttpStatus.OK);
    }

	@RequireUpdateAll
    @PostMapping("dpia/schema/question/{id}/up")
    public ResponseEntity<HttpStatus> reorderQuestionUp(@PathVariable("id") final long id) {
        reorderQuestions(id, false);
        return new ResponseEntity<>(HttpStatus.OK);
    }

	@RequireUpdateAll
    @PostMapping("dpia/schema/question/{id}/down")
    public ResponseEntity<HttpStatus> reorderQuestionDown(@PathVariable("id") final long id) {
        reorderQuestions(id, true);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequireDeleteAll
    @DeleteMapping("dpia/schema/question/{id}/delete")
    public ResponseEntity<HttpStatus> deleteQuestion(@PathVariable("id") final long id) {
        final DPIATemplateQuestion question = dpiaTemplateQuestionService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        question.setDeleted(true);
        dpiaTemplateQuestionService.save(question);
        return new ResponseEntity<>(HttpStatus.OK);
    }

	@RequireDeleteOwnerOnly
    @Transactional
    @DeleteMapping("oversight/{oversightId}")
    public ResponseEntity<HttpStatus> deleteOversight(@PathVariable("oversightId") Long oversightId) {
        final AssetOversight assetOversight = assetService.getOversight(oversightId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if ( !SecurityUtil.isOperationAllowed(Roles.DELETE_ALL) && assetOversight.getResponsibleUser().getUuid().equals(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        assetOversightService.delete(assetOversight);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequireDeleteAll
    @Transactional
    @DeleteMapping("{assetId}/subsupplier/{subSupplierId}")
    public ResponseEntity<HttpStatus> subSupplierDelete(@PathVariable("subSupplierId") final Long subSupplierId,
                                               @PathVariable("assetId") final Long assetId) {
        final Asset asset = assetService.get(assetId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final AssetSupplierMapping subSupplier = asset.getSuppliers().stream().filter(s -> Objects.equals(s.getId(), subSupplierId)).findAny()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        asset.getSuppliers().remove(subSupplier);
        return new ResponseEntity<>(HttpStatus.OK);
    }

	@RequireCreateAll
	@PostMapping("{assetId}/dpia/kitos")
	public ResponseEntity<HttpStatus> syncDPIAToKitos(@PathVariable("assetId") final long assetId, HttpServletRequest request) {
		final Asset asset = assetService.get(assetId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		List<DPIA> dpiaList = asset.getDpias();
		if (dpiaList.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}

		DPIA latestDPIA = dpiaList.stream()
				.max(Comparator.comparing(DPIA::getCreatedAt))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));

		// update last kitos dpia sync property
		asset.getProperties().stream()
				.filter(p -> p.getKey().equals(KITOS_DPIA_LAST_SYNC_PROPERTY_KEY))
				.findFirst()
				.ifPresentOrElse(
						p -> p.setValue(LocalDateTime.now().toString()),
						() -> asset.getProperties().add(Property.builder()
								.key(KITOS_DPIA_LAST_SYNC_PROPERTY_KEY)
								.value(LocalDateTime.now().toString())
								.entity(asset)
								.build())
				);

		// create event
		String kitosUsageId = asset.getProperties().stream().filter(p -> p.getKey().equals(KitosConstants.KITOS_USAGE_UUID_PROPERTY_KEY)).map(Property::getValue).findFirst().orElse(null);
		LocalDateTime createdAt = latestDPIA.getCreatedAt();
		ZoneId zoneId = ZoneId.systemDefault(); // eller en specifik zone fx ZoneId.of("Europe/Copenhagen")
		Date createdAtDate = Date.from(createdAt.atZone(zoneId).toInstant());
		AssetDPIAKitosEvent assetDpiaKitosEvent = AssetDPIAKitosEvent.builder()
				.assetId(assetId)
				.assetKitosItSystemUsageId(kitosUsageId)
				.dpiaDate(createdAtDate)
				.dpiaUrl(request.getScheme() + "://" + request.getServerName() + "/dpia/" + latestDPIA.getId())
				.dpiaName(latestDPIA.getName())
				.build();

		eventPublisher.publishEvent(QueueMessage.builder()
				.queue(KITOS_ASSET_DPIA_CHANGED_QUEUE)
				.priority(1L)
				.body(JsonSimpleMessage.toJson(assetDpiaKitosEvent))
				.build());
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
