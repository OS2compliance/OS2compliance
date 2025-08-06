package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.EmailTemplateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "mail_log", indexes = {
		@Index(name = "idx_mail_log_sent_at", columnList = "sent_at")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MailLog {
	@Id
	private Long id;

	@Column
	private LocalDateTime sentAt;

	@Column
	private String receiver;

	@Column
	@Enumerated(EnumType.STRING)
	private EmailTemplateType templateType;

	@Column
	private String subject;
}
