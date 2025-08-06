package dk.digitalidentity.model.entity.grid;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Table(name = "mail_log")
@Getter
@Setter
@Immutable
public class MailLogGrid {

	@Id
	private Long id;

	@Column
	private LocalDateTime sentAt;

	@Column
	private String receiverName;

	@Column
	private String templateType;

	@Column
	private String subject;
}
