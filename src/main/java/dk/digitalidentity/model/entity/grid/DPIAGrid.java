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
    private LocalDateTime updatedAt;

    @Column
    private int taskCount;
}
