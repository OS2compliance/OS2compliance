package dk.digitalidentity.model.entity.KLE;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

	@OneToMany(mappedBy = "mainGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<KLEGroup> kleGroups = new ArrayList<>();

}