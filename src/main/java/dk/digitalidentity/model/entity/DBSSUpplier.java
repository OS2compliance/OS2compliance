package dk.digitalidentity.model.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import dk.digitalidentity.model.entity.enums.RelationType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dbs_supplier")
@Getter
@Setter
public class DBSSUpplier extends Relatable {

    @Column
    private String name;

    @Column
    @DateTimeFormat(pattern = "dd/MM-yyyy")
    private LocalDate nextRevision;

    //TODO not sure about this
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DBSAsset> assets = new ArrayList<>();

    @Override
    public RelationType getRelationType() {
        return RelationType.DBSSUPPLIER;
    }

    @Override
    public String getLocalizedEnumValues() {
        return "";
    }
}
