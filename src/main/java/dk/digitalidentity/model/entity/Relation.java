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

import java.util.HashSet;
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
	private long id;

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

    @OneToMany(orphanRemoval = true,
        cascade = {CascadeType.ALL},
        mappedBy = "relation")
    private Set<RelationProperty> properties = new HashSet<>();
}
