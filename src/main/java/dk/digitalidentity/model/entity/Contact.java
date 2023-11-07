package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.RelationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "contacts")
@Getter
@Setter
public class Contact extends Relatable {

    @Column
    private String role;

    @Column
    private String phone;

    @Column
    private String mail;

    @Override
    public RelationType getRelationType() {
        return RelationType.CONTACT;
    }

    @Override
    public String getLocalizedEnumValues() {
        return "";
    }
}
