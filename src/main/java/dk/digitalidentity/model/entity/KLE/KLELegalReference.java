package dk.digitalidentity.model.entity.KLE;

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

import java.util.ArrayList;
import java.util.List;

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

	@ManyToMany(mappedBy = "legalReferences", fetch = FetchType.LAZY)
	private List<KLEGroup> groups = new ArrayList<>();

	@ManyToMany(mappedBy = "legalReferences", fetch = FetchType.LAZY)
	private List<KLESubject> subjects = new ArrayList<>();
}
