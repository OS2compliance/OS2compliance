package dk.digitalidentity.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "asset_role")
@Getter
@Setter
@ToString
public class AssetRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="asset_id")
    private Asset asset;

    @ManyToOne
    @JoinColumn(name="role_id")
    private Role role;


    @ManyToMany
    @JoinTable(
        name = "role_assignment",
        joinColumns = { @JoinColumn(name = "user_uuid") },
        inverseJoinColumns = { @JoinColumn(name = "asset_role_id") }
    )
    private Set<User> users = new HashSet<>();
}
