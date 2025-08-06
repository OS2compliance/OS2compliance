package dk.digitalidentity.model.entity.grid;

import dk.digitalidentity.model.entity.enums.EmailTemplateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Table(name = "view_gridjs_mail_log")
@Getter
@Setter
@Immutable
public class MailLogGrid {

	@Id
	private Long id;

	@Column
	private LocalDateTime sentAt;

	@Column
	private String receiver;

	@Column
	@Enumerated(EnumType.STRING)
	private EmailTemplateType type;

	@Column
	private String subject;
}
