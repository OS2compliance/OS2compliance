package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.digitalidentity.model.entity.enums.MeasureTask;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "assets_measures")
@Getter
@Setter
public class AssetMeasure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne
    @JoinColumn(name = "asset_id")
    @JsonIgnore
    private Asset asset;

    @OneToOne
    @JoinColumn(name = "choice_id")
    private ChoiceMeasure measure;

    @Column
    private String answer;

    @Column
    private String note;

    @Column
    @Enumerated(EnumType.STRING)
    private MeasureTask task;

}
