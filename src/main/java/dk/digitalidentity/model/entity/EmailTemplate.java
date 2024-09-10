package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.EmailTemplateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "email_templates")
public class EmailTemplate {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

    @Column
    @NotNull
    private String title;

    @Column
    @NotNull
    private String message;

    @Column
    @Enumerated(EnumType.STRING)
    @NotNull
    private EmailTemplateType templateType;

    @Column
    private boolean enabled;

}
