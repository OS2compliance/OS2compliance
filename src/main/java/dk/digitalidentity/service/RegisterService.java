package dk.digitalidentity.service;

import dk.digitalidentity.dao.ConsequenceAssessmentDao;
import dk.digitalidentity.dao.RegisterDao;
import dk.digitalidentity.dao.grid.RegisterGridDao;
import dk.digitalidentity.model.entity.ConsequenceAssessment;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.grid.RegisterGrid;
import dk.digitalidentity.model.entity.grid.SupplierGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.tag.TagableService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Service
@RequiredArgsConstructor
public class RegisterService implements TagableService<Register> {
    private final RegisterDao registerDao;
	private final RegisterGridDao registerGridDao;
    private final ConsequenceAssessmentDao consequenceAssessmentDao;
	private final RelationService relationService;

	public boolean isResponsibleFor(Register register) {
		return register.getResponsibleUsers().stream().anyMatch(user -> user.getUuid().equals(SecurityUtil.getPrincipalUuid()))
				|| register.getCustomResponsibleUsers().stream().anyMatch(user -> user.getUuid().equals(SecurityUtil.getPrincipalUuid()));
	}

    public Optional<Register> findById(final Long id) {
        return registerDao.findById(id);
    }

    public List<Register> findAllArticle30() {
        return registerDao.findByPackageName("kl_article30");
    }

    public List<Register> findAllByRelations(final List<Relation> relations) {
        final List<Long> lookupIds = relations.stream()
            .map(r -> r.getRelationAType() == RelationType.REGISTER
                ? r.getRelationAId()
                : r.getRelationBId())
            .toList();
        return registerDao.findAllById(lookupIds);
    }

    public List<Register> findAll() {
        return registerDao.findByDeletedFalse();
    }

    public List<Register> findAllOrdered() {
        return registerDao.findByDeletedFalse()
            .stream()
            .sorted((r1, r2) -> {
                final String[] splits1 = StringUtils.split(r1.getName(), " ");
                final String[] splits2 = StringUtils.split(r2.getName(), " ");
                if (splits1.length > 1 && splits2.length > 1) {
                    final String d1 = StringUtils.getDigits(splits1[0]);
                    final String d2 = StringUtils.getDigits(splits2[0]);
                    if (d1.isEmpty() && d2.isEmpty()) {
                        return splits1[0].compareTo(splits2[0]);
                    } else if (!d1.isEmpty() && !d2.isEmpty()) {
                        return (Long.parseLong(d1) > Long.parseLong(d2)) ? 1 : -1;
                    } else if (d1.isEmpty()) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if (splits1.length > 1) {
                    return -1;
                } else if (splits2.length > 1) {
                    return 1;
                }
                return 0;
            })
            .collect(Collectors.toList());
    }

    public boolean existByName(final String title) {
        return registerDao.existsByName(title);
    }

	public Optional<Register> findByNamePrefix(final String prefix) {
		return registerDao.findFirstByNameStartingWithIgnoreCase(prefix);
	}

    public Optional<Register> findByName(final String name) {
        return registerDao.findByNameAndDeletedFalse(name);
    }

    @Transactional
    public Register save(final Register register) {
        if (register.getDataProcessing() == null) {
            register.setDataProcessing(new DataProcessing());
        }
        final Register savedRegister = registerDao.save(register);
        if (savedRegister.getConsequenceAssessment() == null) {
            ConsequenceAssessment consequenceAssessment = new ConsequenceAssessment();
            consequenceAssessment.setRegister(savedRegister);
            consequenceAssessment = consequenceAssessmentDao.save(consequenceAssessment);
            savedRegister.setConsequenceAssessment(consequenceAssessment);
        }
        return registerDao.saveAndFlush(savedRegister);
    }

    @Transactional
    public void delete(final Register register) {
		relationService.deleteRelatedTo(register.getId());
		register.setDataProcessing(null);
		if (register.getConsequenceAssessment() != null) {
			register.getConsequenceAssessment().setRegister(null);
			register.setConsequenceAssessment(null);
		}
        registerDao.delete(register);
    }

	public Set<Register> findAllUnrelatedRegistersForResponsibleUser(User user) {
		return registerDao.findAllByResponsibleUserAndNotRelatedToAnyAsset(user);
	}

	public boolean isInUseOnConsequenceAssessment(Long existingId) {
		return consequenceAssessmentDao.existsByOrganisationAssessmentColumnsChoiceValueId(existingId);
	}

	public boolean isInUseByChoiceValue(long id) {
		return registerDao.existsByStatusId(id);
	}

	public Page<RegisterGrid> getRegisters(String sortColumn, String sortDirection, Map<String, String> filters, int page, int pageLimit, User user) {
		Page<RegisterGrid> registers;
		if (SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
			// Logged-in user can see all
			registers = registerGridDao.findAllWithColumnSearch(
					validateSearchFilters(filters, RegisterGrid.class),
					buildPageable(page, pageLimit, sortColumn, sortDirection),
					RegisterGrid.class
			);
		}
		else {
			// Logged-in user can see only own
			registers = registerGridDao.findAllWithAssignedUser(
					validateSearchFilters(filters, RegisterGrid.class),
					user,
					buildPageable(page, pageLimit, sortColumn, sortDirection),
					RegisterGrid.class
			);
		}
		return registers;
	}

	@Override
	@Transactional
	public Tag addTag(Long entityId, Tag tag) {
		Register entity = registerDao.findById(entityId)
				.orElseThrow(() -> new EntityNotFoundException(Register.class.getSimpleName() + " not found with id: " + entityId));

		entity.getTags().add(tag);
		registerDao.save(entity);

		return tag;
	}

	@Override
	@Transactional
	public Tag removeTag(Long entityId, Long tagId) {
		Register entity = registerDao.findById(entityId)
				.orElseThrow(() -> new EntityNotFoundException(Register.class.getSimpleName() + " not found with id: " + entityId));

		Set<Tag> tags = entity.getTags();
		Tag tag = tags.stream().filter(t -> t.getId() == tagId).findAny().orElse(null);
		if (tag != null) {
			entity.getTags().remove(tag);
			registerDao.save(entity);
		}
		return tag;
	}

	@Override
	public Class<Register> getEntityType() {
		return Register.class;
	}

	@Override
	public Set<Tag> findTagsByEntityId(Long entityId) {
		return registerDao.findTagsByEntityId(entityId);
	}

	@Override
	public Set<Tag> findTagsByEntityIds(Collection<Long> entityIds) {
		return registerDao.findTagsByEntityIds(entityIds);
	}

	public List<RegisterGrid> findByIds(List<Long> ids, User user) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}

		// Fetch all supplier grids by IDs
		List<RegisterGrid> registerGrids = registerGridDao.findAllById(ids);

		// Apply security filtering
		if (SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
			return registerGrids;
		} else {
			// User can only read suppliers they are responsible for
			return registerGrids.stream()
					.filter(rg ->
								rg.getResponsibleUserUuids().contains(user.getUuid()) ||
								rg.getCustomResponsibleUserUuids().contains(user.getUuid())
						   )
					.toList();
		}
	}
}
