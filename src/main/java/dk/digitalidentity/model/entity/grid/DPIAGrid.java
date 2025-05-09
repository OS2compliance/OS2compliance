package dk.digitalidentity.model.entity.grid;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

@Entity
@Table(name = "view_gridjs_dpia")
@Getter
@Setter
@Immutable
public class DPIAGrid {

    @Id
    private Long id;

    @Column
    private String assetName;

    @Column
    private LocalDate userUpdatedDate;

    @Column
    private int taskCount;

    @Column
    private boolean isExternal;
}
