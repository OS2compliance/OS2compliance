package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.RelatableDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.dao.TagDao;
import dk.digitalidentity.mapping.RelatableMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RelatableDTO;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.RelationService;
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
import java.util.Set;

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

    @GetMapping("autocomplete")
    public PageDTO<RelatableDTO> autocomplete(@RequestParam(value = "types", required = false) final List<RelationType> types, @RequestParam("search") final String search) {
        final Pageable page = PageRequest.of(0, 25, Sort.by("createdAt").descending());

        if (types == null || types.isEmpty()) {
            if (StringUtils.length(search) == 0) {
                final Page<Relatable> all = relatableDao.findAll(page);
                return mapper.toDTO(all);
            } else {
                return mapper.toDTO(relatableDao.searchByNameLikeIgnoreCase("%" + search + "%", page));
            }
        } else {
            if (StringUtils.length(search) == 0) {
                final Page<Relatable> all = relatableDao.findByRelationTypeIn(types, page);
                return mapper.toDTO(all);
            } else {
                return mapper.toDTO(relatableDao.searchByRelationTypeInAndNameLikeIgnoreCase(types, "%" + search + "%", page));
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

    record AddRelationDTO(long relatableId, List<Long> relations) {}
    record AddedRelationDTO(long id, String title, RelationType type, String typeForUrl, String typeMessage, String standardIdentifier) {}

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
            .forEach(relation -> relationDao.save(relation));

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
