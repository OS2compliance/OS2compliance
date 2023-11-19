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
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@EntityListeners(AuditingEntityListener.class)
abstract public class Relatable {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private Long id;

	@Version
	@Column(nullable = false)
	private int version;

	@Column(name = "relation_type", nullable = false, updatable = false)
	@Enumerated(EnumType.STRING)
    @Access(AccessType.PROPERTY)
	private RelationType relationType;

	public abstract RelationType getRelationType();

    /**
     * Used for search, return localized enum values, space separated
     */
    public abstract String getLocalizedEnumValues();

	@NotEmpty
	@Column(nullable = false)
    @Access(AccessType.PROPERTY)
	private String name;

	@CreationTimestamp
	@Column(nullable = false)
    @Access(AccessType.PROPERTY)
	private LocalDateTime createdAt;

	@CreatedBy
	@Column(nullable = false)
    @Access(AccessType.PROPERTY)
	private String createdBy;

	@Column
	@UpdateTimestamp
    @Access(AccessType.PROPERTY)
	private LocalDateTime updatedAt;

	@Column
	@LastModifiedBy
    @Access(AccessType.PROPERTY)
	private String updatedBy;

    @Column
    private boolean deleted = false;

    @Column(name = "localized_enums")
    private String localizedEnums;

    @OneToMany(orphanRemoval = true,
			cascade = {CascadeType.ALL},
			mappedBy = "entity")
	private Set<Property> properties = new HashSet<>();

	@PrePersist
	protected void onCreate() {
		relationType = getRelationType();
        localizedEnums = getLocalizedEnumValues();
	}

    @PreUpdate
    protected  void onUpdate() {
        localizedEnums = getLocalizedEnumValues();
    }

}
