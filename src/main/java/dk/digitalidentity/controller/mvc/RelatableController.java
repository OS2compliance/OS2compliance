package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.DocumentDao;
import dk.digitalidentity.dao.RelatableDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.dao.TagDao;
import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.security.RequireUser;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.List;

@Slf4j
@Controller
@RequireUser
@RequestMapping("relatables")
public class RelatableController {
	@Autowired
	private RelatableDao relatableDao;
	@Autowired
	private RelationDao relationDao;
    @Autowired
    private TagDao tagDao;
    @Autowired
    private DocumentDao documentDao;
    @Autowired
    private TaskDao taskDao;

	record AddRelationDTO(long id, List<Long> relations) {}
	@Transactional
	@PostMapping("relations/add")
	public String addRelations(@ModelAttribute final AddRelationDTO dto) {
		final Relatable relateTo = relatableDao.findById(dto.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (dto.relations == null) {
			return getReturnPath(dto.id(), relateTo);
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
		return getReturnPath(dto.id(), relateTo);
	}

	@DeleteMapping("{id}/relations/{relatedId}/{relatedType}/remove")
	@ResponseStatus(value = HttpStatus.OK)
	@Transactional
	public String deleteRelation(@PathVariable final long id, @PathVariable final long relatedId, @PathVariable final RelationType relatedType) {
		final Relatable relatedTo = relatableDao.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		final List<Relation> related = relationDao.findAllRelatedTo(relatedTo.getId());
		final Relation toDelete = related.stream()
            .filter(r -> (r.getRelationAId() == relatedId && r.getRelationAType().equals(relatedType)) || (r.getRelationBId() == relatedId && r.getRelationBType().equals(relatedType)))
            .findAny()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relateret entitet ikke fundet"));
		relationDao.delete(toDelete);
		return getReturnPath(id, relatedTo);
	}

    record AddTagsDTO(long id, List<Long> tags) {}
    @Transactional
    @PostMapping("tags/add")
    public String addTags(@ModelAttribute final AddTagsDTO dto) {
        final Relatable relateTo = relatableDao.findById(dto.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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

            documentDao.save(document);
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

            taskDao.save(task);
        }

        return getReturnPath(dto.id(), relateTo);
    }

    @DeleteMapping("{id}/tags/{tagId}/remove")
    @ResponseStatus(value = HttpStatus.OK)
    @Transactional
    public String deleteRelation(@PathVariable final long id, @PathVariable final long tagId) {
        final Relatable relatedTo = relatableDao.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final Tag tagToDelete = tagDao.findById(tagId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (relatedTo.getRelationType().equals(RelationType.DOCUMENT)) {
            final Document document = (Document) relatedTo;
            document.getTags().remove(tagToDelete);
            documentDao.save(document);
        } else if (relatedTo.getRelationType().equals(RelationType.TASK)) {
            final Task task = (Task) relatedTo;
            task.getTags().remove(tagToDelete);
            taskDao.save(task);
        }
        return "redirect:/";
    }

    @NotNull private String getReturnPath(final long id, final Relatable relatable) {
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
}
