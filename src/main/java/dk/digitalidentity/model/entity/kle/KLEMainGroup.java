package dk.digitalidentity.model.entity.kle;

import dk.digitalidentity.model.entity.Register;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
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

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "kle_main_group")
public class KLEMainGroup {

	@Id
	@Column(name = "main_group_number")
	private String mainGroupNumber;

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

	@OneToMany(mappedBy = "mainGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@Builder.Default
	private Set<KLEGroup> kleGroups = new HashSet<>();

	@ManyToMany(mappedBy = "kleMainGroups",
			cascade = {CascadeType.PERSIST, CascadeType.MERGE},
			fetch = FetchType.LAZY
	)
	@Builder.Default
	private Set<Register> registers = new HashSet<>();
}