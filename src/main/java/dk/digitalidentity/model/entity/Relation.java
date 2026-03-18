package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.RelationType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "relations")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Relation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "relation_a_id")
	private Long relationAId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, name = "relation_a_type")
	private RelationType relationAType;

	@Column(name = "relation_b_id")
	private Long relationBId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, name = "relation_b_type")
	private RelationType relationBType;

	@Column(name = "relation_a_name")
	private String relationAName;

	@Column(name = "relation_b_name")
	private String relationBName;

    @OneToMany(orphanRemoval = true,
        cascade = {CascadeType.ALL},
        mappedBy = "relation")
	@Builder.Default
    private Set<RelationProperty> properties = new HashSet<>();

	@Override
	public final boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
		Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
		Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
		if (thisEffectiveClass != oEffectiveClass)
			return false;
		Relation relation = (Relation) o;
		return getId() != null && Objects.equals(getId(), relation.getId());
	}

	@Override
	public final int hashCode() {
		return this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
	}
}
