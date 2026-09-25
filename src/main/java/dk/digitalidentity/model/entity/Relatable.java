package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.RelationType;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Audited
@EntityListeners(AuditingEntityListener.class)
public abstract class Relatable {

	/**
	 * The name of the id generator shared by this hierarchy and {@link CustomThreat}. It is declared
	 * here and referenced from there so both keep drawing from the same {@code default} segment.
	 */
	public static final String ID_GENERATOR = "shared_id_generator";

	/**
	 * Spelled out to silence HHH000398 on every startup. Every value matches what Hibernate already
	 * defaulted to, so id allocation is unchanged - see TableGenerator.DEF_* and
	 * OptimizableGenerator.DEFAULT_INCREMENT_SIZE. {@code pkColumnValue} in particular must stay: drop
	 * it and Hibernate derives a segment per entity, restarting allocation at 1.
	 * <p>
	 * Because {@link InheritanceType#TABLE_PER_CLASS} makes Hibernate key entities on (id, Relatable),
	 * an id handed out twice within this hierarchy surfaces as a ClassCastException between two
	 * subclasses. Two rules follow, and V1_52__fix_dpia_ids.sql broke both:
	 * <ul>
	 * <li>Never give a subclass its own segment.
	 * <li>Never assign ids from {@code next_val}. It is not the highest id in use, and a fresh block
	 * starts <em>below</em> it: the generator writes next_val + allocationSize, returns next_val + 1,
	 * and PooledOptimizer then hands out [returned - (allocationSize - 1) .. returned]. Renumbering
	 * rows to next_val + 1 and up therefore lands them inside the very next block. If a data fix has
	 * to move rows, leave at least allocationSize of room above them and push next_val past that.
	 * </ul>
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = ID_GENERATOR)
	@TableGenerator(
			name = ID_GENERATOR,
			table = "hibernate_sequences",
			pkColumnName = "sequence_name",
			valueColumnName = "next_val",
			pkColumnValue = "default",
			allocationSize = 50
	)
	private Long id;

	@Version
	@Column(nullable = false)
	private int version;

	private RelationType relationType;
	@Column(name = "relation_type", nullable = false, updatable = false)
	@Enumerated(EnumType.STRING)
	@Access(AccessType.PROPERTY)
	public abstract RelationType getRelationType();

    /**
     * Used for search, return localized enum values, space separated
     */
    public abstract String getLocalizedEnumValues();

	private String name;

	@NotEmpty
	@Column(nullable = false)
	@Access(AccessType.PROPERTY)
	public String getName() { return name; }

	private LocalDateTime createdAt;
	@CreationTimestamp
	@Access(AccessType.PROPERTY)
	@Column(name = "created_at", nullable = false, updatable = false)
	public LocalDateTime getCreatedAt() { return createdAt; }

	private String createdBy;
	@Column(name = "created_by", nullable = false)
	@Access(AccessType.PROPERTY)
	public String getCreatedBy() { return createdBy; }

	private LocalDateTime updatedAt;
	@Column
	@UpdateTimestamp
	@Access(AccessType.PROPERTY)
	public LocalDateTime getUpdatedAt() { return updatedAt; }

	private String updatedBy;
	@Column
	@LastModifiedBy
	@Access(AccessType.PROPERTY)
	public String getUpdatedBy() { return updatedBy; }

    @Column
    private boolean deleted = false;

    @Column(name = "localized_enums")
    private String localizedEnums;

    @OneToMany(orphanRemoval = true,
			cascade = {CascadeType.ALL},
			mappedBy = "entity")
	@NotAudited
	private Set<Property> properties = new HashSet<>();

	@PrePersist
	protected void onCreate() {
		relationType = getRelationType();
        localizedEnums = getLocalizedEnumValues();
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		if (updatedAt == null) {
			updatedAt = createdAt;
		}
		if (createdBy == null) {
			createdBy = "system";
		}
	}

    @PreUpdate
    protected  void onUpdate() {
        localizedEnums = getLocalizedEnumValues();
    }

}
