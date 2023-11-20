package dk.digitalidentity.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "settings")
public class Setting {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "setting_key")
	private String settingKey;

	@Column(name = "setting_value")
	private String settingValue;

    @Column(name = "editable")
    private Boolean editable;

    @Column(name = "association")
    private String association;

	@Column(name = "last_updated")
	private LocalDateTime lastUpdated;

}
