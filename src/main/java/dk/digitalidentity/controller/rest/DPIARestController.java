package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.ChoiceDPIADao;
import dk.digitalidentity.dao.grid.DPIAGridDao;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.DataProtectionImpactAssessmentScreening;
import dk.digitalidentity.model.entity.DataProtectionImpactScreeningAnswer;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.DPIAGrid;
import dk.digitalidentity.security.RequireSuperuserOrAdministrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.DPIAService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

@Slf4j
@RestController
@RequestMapping("rest/dpia")
@RequireUser
@RequiredArgsConstructor
public class DPIARestController {
	private final DPIAGridDao dpiaGridDao;
	private final DPIAService dpiaService;
	private final ChoiceDPIADao choiceDPIADao;
    private final AssetService assetService;

	public record DPIADTO(long id, String assetName, LocalDateTime updatedAt, int taskCount) {
	}

	@PostMapping("list")
	public PageDTO<DPIADTO> list(
			@RequestParam(name = "search", required = false) final String search,
			@RequestParam(name = "page", required = false, defaultValue = "0") final Integer page,
			@RequestParam(name = "size", required = false, defaultValue = "50") final Integer size,
			@RequestParam(name = "order", required = false) final String order,
			@RequestParam(name = "dir", required = false) final String dir
	) {
		Sort sort = null;
		if (isNotEmpty(order)) {
			final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
			sort = Sort.by(direction, order);
		}
		final Pageable sortAndPage = sort != null ? PageRequest.of(page, size, sort) : PageRequest.of(page, size);
		final Page<DPIAGrid> dpiaGrids;
		if (StringUtils.isNotEmpty(search)) {
			final List<String> searchableProperties = Arrays.asList("assetName", "updatedAt");
			dpiaGrids = dpiaGridDao.findAllCustom(searchableProperties, search, sortAndPage, DPIAGrid.class);
		} else {
			// Fetch paged and sorted
			dpiaGrids = dpiaGridDao.findAll(sortAndPage);
		}
		assert dpiaGrids != null;
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return new PageDTO<>(dpiaGrids.getTotalElements(), dpiaGrids.stream().map(dpia ->
						new DPIADTO(
								dpia.getId(),
								dpia.getAssetName(),
								dpia.getUpdatedAt(),
								dpia.getTaskCount()
						))
				.toList());
	}

	@DeleteMapping("delete/{id}")
	@RequireSuperuserOrAdministrator
	@ResponseStatus(value = HttpStatus.OK)
	@Transactional
	public void delete(@PathVariable final Long id) {
		dpiaService.delete(id);
	}

	public record DPIAScreeningUpdateDTO(Long assetId, String answer, String choiceIdentifier) {
	}

	@Transactional
	@PostMapping("screening/update")
	public ResponseEntity<HttpStatus> dpia(@RequestBody final DPIAScreeningUpdateDTO dpiaScreeningUpdateDTO) {
		final Asset asset = assetService.findById(dpiaScreeningUpdateDTO.assetId)
            .orElseThrow();
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(SecurityUtil.getPrincipalUuid())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		final DataProtectionImpactAssessmentScreening dpiaScreening = asset.getDpiaScreening();

			final DataProtectionImpactScreeningAnswer foundAnswer = dpiaScreening.getDpiaScreeningAnswers().stream()
					.filter(a -> a.getChoice().getIdentifier().equalsIgnoreCase(dpiaScreeningUpdateDTO.choiceIdentifier))
					.findFirst()
					.orElseGet(() -> {
						final DataProtectionImpactScreeningAnswer newAnswer = DataProtectionImpactScreeningAnswer.builder()
								.assessment(dpiaScreening)
								.choice(choiceDPIADao.findByIdentifier(dpiaScreeningUpdateDTO.choiceIdentifier).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)))
								.build();
						dpiaScreening.getDpiaScreeningAnswers().add(newAnswer);
						return newAnswer;
					});
			foundAnswer.setAnswer(dpiaScreeningUpdateDTO.answer);

		return new ResponseEntity<>(HttpStatus.OK);
	}

    @Transactional
    public record CommentUpdateDTO(Long assetId, String comment){}
    @PostMapping("comment/update")
    public ResponseEntity<HttpStatus> updateDPIAComment(@RequestBody final CommentUpdateDTO commentUpdateDTO) {
        final Asset asset = assetService.findById(commentUpdateDTO.assetId)
            .orElseThrow();
        final DPIA dpia = asset.getDpia();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        dpia.setComment(commentUpdateDTO.comment);
        assetService.save(asset);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public record QualityAssuranceUpdateDTO (Long assetId, Set<String> dpiaQualityCheckValues) {}
    @Transactional
	@PostMapping("qualityassurance/update")
	public ResponseEntity<HttpStatus> dpia(@RequestBody final QualityAssuranceUpdateDTO qualityAssuranceUpdateDTO) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		final Asset asset = assetService.findById(qualityAssuranceUpdateDTO.assetId)
            .orElseThrow();
		if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(SecurityUtil.getPrincipalUuid())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

        asset.getDpia().setChecks(qualityAssuranceUpdateDTO.dpiaQualityCheckValues);

		return new ResponseEntity<>(HttpStatus.OK);
	}

    public record CreateDPIAFormDTO (Long assetId){}
    @PostMapping("create")
    public ResponseEntity<HttpStatus> createDpia (@RequestBody final  CreateDPIAFormDTO createDPIAFormDTO){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Asset asset = assetService.findById(createDPIAFormDTO.assetId)
            .orElseThrow();
        if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        //TODO - når assets kan supporte flere dpia'er skal dette laves om
        if (asset.getDpia() == null) {
            DPIA dpia = new DPIA();
            dpia.setName(asset.getName()+" Konsekvensaanalyse");
            dpia.setAsset(asset);
            asset.setDpia(dpia);
            assetService.save(asset);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

	public record CreateExternalDPIADTO (Long assetId, String link){}
	@PostMapping("create")
	public ResponseEntity<HttpStatus> createExternalDpia (@RequestBody final  CreateExternalDPIADTO createExternalDPIADTO){
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		final Asset asset = assetService.findById(createExternalDPIADTO.assetId)
				.orElseThrow();
		if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(SecurityUtil.getPrincipalUuid())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		asset.getDpiaScreening().setConsequenceLink(createExternalDPIADTO.link);
		assetService.save(asset);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
