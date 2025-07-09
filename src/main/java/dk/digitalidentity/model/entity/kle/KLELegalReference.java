package dk.digitalidentity.model.entity.kle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "kle_legal_reference")
public class KLELegalReference {

	@Id
	@Column(name = "accession_number")
	private String accessionNumber;

	@Column(name = "paragraph", columnDefinition = "TEXT")
	private String paragraph;

	@Column(name = "url")
	private String url;

	@Column(name = "title", nullable = false)
	private String title;

	@Column
	private boolean deleted;

	@ManyToMany(mappedBy = "legalReferences", fetch = FetchType.LAZY)
	private Set<KLEGroup> groups = new HashSet<>();

	@ManyToMany(mappedBy = "legalReferences", fetch = FetchType.LAZY)
	private Set<KLESubject> subjects = new HashSet<>();
}
