package dk.digitalidentity.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "positions")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    private String ouUuid;

    @Column
    private String name;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
}
