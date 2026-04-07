package dk.digitalidentity.model.entity;

import dk.digitalidentity.config.TagColorConverter;
import dk.digitalidentity.model.dto.enums.TagColor;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "tags")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    private String value;

	@Builder.Default
	@Convert(converter = TagColorConverter.class)
	@Column(name = "color_hex_code", nullable = false)
	private TagColor color = TagColor.GREY;

	@Column(name = "year_wheel")
	private boolean yearWheel = false;
}
