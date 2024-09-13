package dk.digitalidentity.model.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dbs_supplier")
@Getter
@Setter
public class DBSSupplier {
    
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long id;

    @Column
    private Long dbsId;

    @Column
    private String name;

    @Column
    @DateTimeFormat(pattern = "dd/MM-yyyy")
    private LocalDate nextRevision;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DBSAsset> assets = new ArrayList<>();

}
