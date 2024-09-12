package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.RelationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dbs_asset")
@Getter
@Setter
public class DBSAsset extends Relatable {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dbs_supplier")
    private DBSSUpplier supplier;

    @Column
    private String name;

    @Column
    private boolean applicable;

    @Override
    public RelationType getRelationType() {
        return RelationType.DBSASSET;
    }

    @Override
    public String getLocalizedEnumValues() {
        return "";
    }
}
