package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "role")
@Getter
@Setter
@ToString
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @ManyToOne
    @JoinColumn(name="asset_id")
    private Asset asset;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
        name = "user_role",
        joinColumns = { @JoinColumn(name = "user_uuid") },
        inverseJoinColumns = { @JoinColumn(name = "role_id") }
    )
    private Set<User> users = new HashSet<>();
}
