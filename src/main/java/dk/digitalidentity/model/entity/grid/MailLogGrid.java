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
@Table(name = "view_mail_log_grid")
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
	private String type;

	@Column
	private String subject;
}
