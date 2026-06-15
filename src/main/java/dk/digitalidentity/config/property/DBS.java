package dk.digitalidentity.config.property;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DBS {
    private boolean enabled = true;
	private LocalDate backfillFrom;
}
