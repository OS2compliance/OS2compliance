package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.TagDao;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.RelationProperty;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.DocumentService;
import dk.digitalidentity.service.RelatableService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("ClassEscapesDefinedScope")
@Slf4j
@Controller
@RequireUser
@RequestMapping("relatables")
@RequiredArgsConstructor
public class RelatableController {
    private final RelatableService relatableService;
    private final RelationService relationService;
    private final DocumentService documentService;
    private final TaskService taskService;
    private final TagDao tagDao;

	record AddRelationDTO(long id, List<Long> relations, Map<String, String> properties) {
        public AddRelationDTO {
            properties = new HashMap<>();
        }
    }
	@Transactional
	@PostMapping("relations/add")
	public String addRelations(@ModelAttribute final AddRelationDTO dto) {
		final Relatable relateTo = relatableService.findById(dto.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (dto.relations == null) {
			return getReturnPath(dto.id(), relateTo);
		}

		dto.relations().stream()
				.map(relatedId -> relatableService.findById(relatedId)
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relateret entitet ikke fundet")))
				.map(relatable -> Relation.builder()
						.relationAId(relateTo.getId())
						.relationAType(relateTo.getRelationType())
						.relationBId(relatable.getId())
						.relationBType(relatable.getRelationType())
                        .properties(new HashSet<>())
						.build())
				.map(relationService::save)
                .forEach(relation -> setRelationProperties(relation, dto.properties));
		return getReturnPath(dto.id(), relateTo);
	}


    record UpdateRelationDTO(long id, Map<String, String> properties) {
        public UpdateRelationDTO {
            properties = new HashMap<>();
        }
    }
    @Transactional
    @PostMapping("{id}/relations/{relatedId}/{relatedType}/update")
    public String updateRelation(@ModelAttribute final UpdateRelationDTO dto,
                                 @PathVariable final long id, @PathVariable final long relatedId, @PathVariable final RelationType relatedType) {
        final Relatable relatedTo = relatableService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final Relation toUpdate = relationService.findRelationEntity(relatedTo, relatedId, relatedType);
        setRelationProperties(toUpdate, dto.properties);
        return getReturnPath(id, relatedTo);
    }

    @DeleteMapping("{id}/relations/{relatedId}/{relatedType}/remove")
	@ResponseStatus(value = HttpStatus.OK)
	@Transactional
	public String deleteRelation(@PathVariable final long id, @PathVariable final long relatedId, @PathVariable final RelationType relatedType) {
		final Relatable relatedTo = relatableService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final Relation toDelete = relationService.findRelationEntity(relatedTo, relatedId, relatedType);
		relationService.delete(toDelete);
		return getReturnPath(id, relatedTo);
	}

    record AddTagsDTO(long id, List<Long> tags) {}
    @Transactional
    @PostMapping("tags/add")
    public String addTags(@ModelAttribute final AddTagsDTO dto) {
        final Relatable relateTo = relatableService.findById(dto.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (dto.tags == null) {
            return getReturnPath(dto.id(), relateTo);
        }

        if (relateTo.getRelationType().equals(RelationType.DOCUMENT)) {
            final Document document = (Document) relateTo;
            if (document.getTags() == null) {
                document.setTags(new ArrayList<>());
            }

            final List<Long> addedTags = document.getTags().stream().map(Tag::getId).toList();
            for (final Long tag : dto.tags) {
                if (!addedTags.contains(tag)) {
                    tagDao.findById(tag).ifPresent(dbTag -> document.getTags().add(dbTag));
                }
            }

            documentService.create(document);
        }
        else if (relateTo.getRelationType().equals(RelationType.TASK)) {
            final Task task = (Task) relateTo;
            if (task.getTags() == null) {
                task.setTags(new ArrayList<>());
            }

            final List<Long> addedTags = task.getTags().stream().map(Tag::getId).toList();
            for (final Long tag : dto.tags) {
                if (!addedTags.contains(tag)) {
                    tagDao.findById(tag).ifPresent(dbTag -> task.getTags().add(dbTag));
                }
            }

            taskService.saveTask(task);
        }

        return getReturnPath(dto.id(), relateTo);
    }

    @DeleteMapping("{id}/tags/{tagId}/remove")
    @ResponseStatus(value = HttpStatus.OK)
    @Transactional
    public String deleteRelation(@PathVariable final long id, @PathVariable final long tagId) {
        final Relatable relatedTo = relatableService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final Tag tagToDelete = tagDao.findById(tagId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (relatedTo.getRelationType().equals(RelationType.DOCUMENT)) {
            final Document document = (Document) relatedTo;
            document.getTags().remove(tagToDelete);
            documentService.create(document);
        } else if (relatedTo.getRelationType().equals(RelationType.TASK)) {
            final Task task = (Task) relatedTo;
            task.getTags().remove(tagToDelete);
            taskService.saveTask(task);
        }
        return "redirect:/";
    }

    private String getReturnPath(final long id, final Relatable relatable) {
        if (relatable.getRelationType().equals(RelationType.DOCUMENT)) {
            return "redirect:/documents/" + id;
        }
        else if (relatable.getRelationType().equals(RelationType.TASK)) {
            return "redirect:/tasks/" + id;
        }
        else if (relatable.getRelationType().equals(RelationType.ASSET)) {
            return "redirect:/assets/" + id;
        }
        else if (relatable.getRelationType().equals(RelationType.SUPPLIER)) {
            return "redirect:/suppliers/" + id;
        }
        else if (relatable.getRelationType().equals(RelationType.REGISTER)) {
            return "redirect:/registers/" + id;
        }
        else {
            return "redirect:/";
        }
    }


    private void setRelationProperties(final Relation relation, final Map<String, String> properties) {
        if (properties != null) {
            relation.getProperties().clear();
            final Set<RelationProperty> relationProperties = properties.entrySet().stream().map(e -> RelationProperty.builder()
                    .relation(relation)
                    .key(e.getKey())
                    .value(e.getValue())
                    .build())
                .collect(Collectors.toSet());
            relation.getProperties().addAll(relationProperties);
        }
    }

}
