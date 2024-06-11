package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.RelationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "precautions")
@Getter
@Setter
@ToString
@SQLDelete(sql = "UPDATE precautions SET deleted = true WHERE id=? and version=?", check = ResultCheckStyle.COUNT)
@Where(clause = "deleted=false")
public class Precaution extends Relatable {

    @Column
    private String description;

    @Override
    public RelationType getRelationType() {
        return RelationType.PRECAUTION;
    }

    @Override
    public String getLocalizedEnumValues() {
        return "";
    }
}
