package dk.digitalidentity.model.entity.KLE;

import dk.digitalidentity.config.DurationConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "kle_subject")
public class KLESubject {

	@Id
	@Column(name = "subject_number")
	private String subjectNumber;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "instruction_text", columnDefinition = "TEXT")
	private String instructionText;

	@Column(name = "creation_date")
	private LocalDate creationDate;

	@Column(name = "last_update_date")
	private LocalDate lastUpdateDate;

	@Column(name = "preservation_code")
	private String preservationCode;

	@Convert(converter = DurationConverter.class)
	@Column(name = "duration_before_deletion")
	private Duration durationBeforeDeletion;

	@Column(name = "uuid")
	private String uuid;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "group_number", referencedColumnName = "group_number")
	private KLEGroup group;

	@ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinTable(
			name = "kle_subject_legal_reference",
			joinColumns = @JoinColumn(name = "subject_number"),
			inverseJoinColumns = @JoinColumn(name = "accession_number")
	)
	private List<KLELegalReference> legalReferences = new ArrayList<>();

}
