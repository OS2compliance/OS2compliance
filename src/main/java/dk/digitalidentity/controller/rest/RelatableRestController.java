package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.RelatableDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.dao.TagDao;
import dk.digitalidentity.mapping.RelatableMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RelatableDTO;
import dk.digitalidentity.model.entity.CustomThreat;
import dk.digitalidentity.model.entity.Precaution;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.ThreatDatabaseType;
import dk.digitalidentity.security.RequireSuperuserOrAdministrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.PrecautionService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.ThreatAssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


@SuppressWarnings("ClassEscapesDefinedScope")
@Slf4j
@RestController
@RequestMapping("rest/relatable")
@RequireUser
@RequiredArgsConstructor
public class RelatableRestController {
    private final RelatableDao relatableDao;
    private final RelatableMapper mapper;
    private final RelationDao relationDao;
    private final RelationService relationService;
    private final TagDao tagDao;
    private final ThreatAssessmentService threatAssessmentService;
    private final AssetService assetService;
    private final PrecautionService precautionService;

    @GetMapping("autocomplete")
    public PageDTO<RelatableDTO> autocomplete(@RequestParam(value = "types", required = false) final List<RelationType> types,
                                              @RequestParam(value = "search") final String search,
                                              @RequestParam(value = "sort", defaultValue = "createdAt") final String orderBy,
                                              @RequestParam(value = "dir", defaultValue = "DESC") final String direction) {
        final Pageable page = PageRequest.of(0, 25, Sort.by(Sort.Direction.fromString(direction), orderBy));

        if (types == null || types.isEmpty()) {
            if (StringUtils.length(search) == 0) {
                final Page<Relatable> all = relatableDao.findAllByDeletedFalse(page);
                return mapper.toDTO(all);
            } else {
                return mapper.toDTO(relatableDao.searchByNameLikeIgnoreCaseAndDeletedFalse("%" + search + "%", page));
            }
        } else {
            if (StringUtils.length(search) == 0) {
                final Page<Relatable> all = relatableDao.findByRelationTypeInAndDeletedFalse(types, page);
                return mapper.toDTO(all);
            } else {
                return mapper.toDTO(relatableDao.searchByRelationTypeInAndNameLikeIgnoreCaseAndDeletedFalse(types, "%" + search + "%", page));
            }
        }

    }

    @GetMapping("tags/autocomplete")
    public PageDTO<Tag> autocomplete(@RequestParam("search") final String search) {
        final Pageable page = PageRequest.of(0, 25, Sort.by("value").descending());
        if (StringUtils.length(search) == 0) {
            final Page<Tag> all = tagDao.findAll(page);
            return new PageDTO<>(all.getTotalElements(), all.getContent());
        } else {
            final Page<Tag> result = tagDao.searchByValueLikeIgnoreCase("%" + search + "%", page);
            return new PageDTO<>(result.getTotalElements(), result.getContent());
        }
    }

	public record RelatedPrecautionDTO(Long id,String name,String type,String typeMessage,String description){}
    @GetMapping("autocomplete/relatedprecautions")
    public PageDTO<RelatedPrecautionDTO> autocompletePrecautions(@RequestParam("search") final String search, @RequestParam("threatId") final long threatId, @RequestParam("threatType") final ThreatDatabaseType threatType, @RequestParam("threatIdentifier") final String threatIdentifier, @RequestParam("riskId") final long riskId) {
        final ThreatAssessment threatAssessment = threatAssessmentService.findById(riskId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (threatAssessment.getThreatAssessmentResponses() == null) {
            threatAssessment.setThreatAssessmentResponses(new ArrayList<>());
        }

        ThreatAssessmentResponse response;
		if (threatAssessment.isFromExternalSource()) {
			return new PageDTO<>(0L, new ArrayList<>());
		} else if (threatType.equals(ThreatDatabaseType.CATALOG)) {
			//find the relevant threat in the catalog
            final ThreatCatalogThreat threat = threatAssessment.getThreatCatalog().getThreats().stream()
					.filter(t -> t.getIdentifier().equals(threatIdentifier))
					.findAny()
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

            response = threatAssessment.getThreatAssessmentResponses().stream()
                .filter(r -> r.getThreatCatalogThreat() != null && r.getThreatCatalogThreat().getIdentifier().equals(threat.getIdentifier()))
                .findAny()
                .orElse(null);
			//If no response to the threat is found, create a blank one
            if (response == null) {
                response = threatAssessmentService.createResponse(threatAssessment, threat, null);
            }
        } else if (threatType.equals(ThreatDatabaseType.CUSTOM)) {
			//find the relevant threat in the catalog
            final CustomThreat threat = threatAssessment.getCustomThreats().stream()
					.filter(t -> t.getId().equals(threatId))
					.findAny()
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            response = threatAssessment.getThreatAssessmentResponses().stream()
                .filter(r -> r.getCustomThreat() != null && Objects.equals(r.getCustomThreat().getId(), threat.getId()))
                .findAny()
                .orElse(null);
			//If no response to the threat is found, create a blank one
            if (response == null) {
                response = threatAssessmentService.createResponse(threatAssessment, null, threat);
            }
        } else {
            return new PageDTO<>(0L, new ArrayList<>());
        }

        final List<RelationType> relationTypes = new ArrayList<>();
        relationTypes.add(RelationType.PRECAUTION);
        if (search.isBlank()) {

            // Find any precautions related to assets that is related to the threatassesment
            final List<Long> relatedAssets = relationService.findRelatedToWithType(threatAssessment, RelationType.ASSET).stream()
					.map(ar -> ar.getRelationAType().equals(RelationType.ASSET) ? ar.getRelationAId() : ar.getRelationBId())
					.toList();

			List<Long> relatedPrecautionIds = relationService.findRelatedToWithType(relatedAssets, RelationType.PRECAUTION).stream()
					.map(re -> re.getRelationAType().equals(RelationType.PRECAUTION) ? re.getRelationAId() : re.getRelationBId())
					.toList();

			// Sort all precautions, with the ones related to related assets at the top, by name otherwise
			final List<RelatedPrecautionDTO> sortedContent = precautionService.findAll().stream()
					.sorted((a, b) -> {
						boolean aRelatedToAsset = relatedPrecautionIds.contains(a.getId());
						boolean bRelatedToAsset = relatedPrecautionIds.contains(b.getId());
						if (aRelatedToAsset && !bRelatedToAsset) {
							return -1;
						} else if (!aRelatedToAsset && bRelatedToAsset) {
							return 1;
						} else {
							//compare both by name if they both are related, or if none of them are
							return a.getName().compareToIgnoreCase(b.getName());
						}
					})
					.map(p -> new RelatedPrecautionDTO(
							p.getId(),
							p.getName(),
							p.getRelationType().toString(),
							p.getRelationType().getMessage(),
							p.getDescription()
					))
					.toList();

                return new PageDTO<>((long)sortedContent.size(),  sortedContent);
        } else {
        	final Pageable page = PageRequest.of(0, 50, Sort.by("name").descending());
			var searchResult = relatableDao.searchByRelationTypeInAndNameLikeIgnoreCaseAndDeletedFalse(relationTypes, "%" + search + "%", page);
            return new PageDTO<>((long)searchResult.getSize(), searchResult.stream().map(p -> new RelatedPrecautionDTO(p.getId(), p.getName(), p.getRelationType().toString(), p.getRelationType().getMessage(), ((Precaution)p).getDescription())).toList());
        }
    }

    record AddRelationDTO(long relatableId, List<Long> relations) {}
    record AddedRelationDTO(long id, String title, RelationType type, String typeForUrl, String typeMessage, String standardIdentifier) {}

    @RequireSuperuserOrAdministrator
    @PostMapping("relations/add")
    public ResponseEntity<?> addRelation(@Valid @RequestBody final AddRelationDTO dto) {
        final Relatable relateTo = relatableDao.findById(dto.relatableId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (dto.relations == null || dto.relations.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.OK);
        }

        dto.relations().stream()
            .map(relatedId -> relatableDao.findById(relatedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relateret entitet ikke fundet")))
            .map(relatable -> Relation.builder()
                .relationAId(relateTo.getId())
                .relationAType(relateTo.getRelationType())
                .relationBId(relatable.getId())
                .relationBType(relatable.getRelationType())
                .build())
            .forEach(relationDao::save);

        final Set<Long> addedIds = new HashSet<>();
        final List<AddedRelationDTO> relationsToReturn = new ArrayList<>();
        for (final Relatable relatable : relationService.findAllRelatedTo(relateTo)) {
            if (dto.relations.contains(relatable.getId()) && !addedIds.contains(relatable.getId())) {
                if (relatable.getRelationType().equals(RelationType.STANDARD_SECTION)) {
                    final StandardSection asStandardSection = (StandardSection) relatable;
                    relationsToReturn.add(new AddedRelationDTO(relatable.getId(), relatable.getName(), relatable.getRelationType(),
                        findTypeForUrl(relatable.getRelationType()),
                        relatable.getRelationType().getMessage(),
                        RelationService.extractStandardIdentifier(asStandardSection)
                    ));
                } else {
                    relationsToReturn.add(new AddedRelationDTO(relatable.getId(), relatable.getName(), relatable.getRelationType(), findTypeForUrl(relatable.getRelationType()), relatable.getRelationType().getMessage(), null));
                }
                addedIds.add(relatable.getId());
            }
        }

        return new ResponseEntity<>(relationsToReturn, HttpStatus.OK);
    }

    private String findTypeForUrl(final RelationType relationType) {
        return switch (relationType) {
            case SUPPLIER -> "suppliers";
            case CONTACT -> "contacts";
            case TASK -> "tasks";
            case DOCUMENT -> "documents";
            case REGISTER -> "registers";
            case ASSET -> "assets";
            case THREAT_ASSESSMENT -> "risks";
            case STANDARD_SECTION -> "standards/supporting";
            default ->
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ukendt relationType: " + relationType);
        };
    }
}
