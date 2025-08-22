package dk.digitalidentity.model.entity.kle;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Builder
@Getter
@Setter
@Entity
@Table(name = "KLE_group")
@NoArgsConstructor
@AllArgsConstructor
public class KLEGroup {

	@Id
	@Column(name = "group_number")
	private String groupNumber;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "instruction_text", columnDefinition = "TEXT")
	private String instructionText;

	@Column(name = "creation_date")
	private LocalDate creationDate;

	@Column(name = "last_update_date")
	private LocalDate lastUpdateDate;

	@Column(name = "uuid")
	private String uuid;

	@Column
	private boolean deleted;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "main_group_number", referencedColumnName = "main_group_number")
	private KLEMainGroup mainGroup;

	@Builder.Default
	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private Set<KLESubject> subjects = new HashSet<>();

	@ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinTable(
			name = "kle_group_legal_reference",
			joinColumns = @JoinColumn(name = "group_number"),
			inverseJoinColumns = @JoinColumn(name = "accession_number")
	)
	@Builder.Default
	private Set<KLELegalReference> legalReferences = new HashSet<>();

}
