package dk.digitalidentity.model.entity.kle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "kle_keyword")
public class KLEKeyword implements Persistable<String> {

	@Id
	@Column
	private String hashedId;

	@Column
	private String text;

	@Column
	private String handlingsfacetNr;

	@ManyToMany(mappedBy = "keywords", fetch = FetchType.LAZY)
	private Set<KLEGroup> groups = new HashSet<>();

	@ManyToMany(mappedBy = "keywords", fetch = FetchType.LAZY)
	private Set<KLESubject> subjects = new HashSet<>();

	@Transient
	private boolean isNew = true;

	public void setAsNew() {
		isNew = true;
	}

	@Override
	public String getId() {
		return hashedId;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PrePersist
	@PostLoad
	public void markNotNew() {
		this.isNew = false;
	}

	// Helper method for your sync logic
	public void markAsExisting() {
		this.isNew = false;
	}
}
