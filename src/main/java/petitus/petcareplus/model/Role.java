package petitus.petcareplus.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NaturalId;
import petitus.petcareplus.utils.Constants;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name"}, name = "uk_roles_name")
}, indexes = {
        @Index(columnList = "name", name = "idx_roles_name")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role extends AbstractBaseEntity {
    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @Builder.Default
    @JsonManagedReference
    private Set<User> users = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "name", length = 16)
    @NaturalId
    private Constants.RoleEnum name;

    public Role(final Constants.RoleEnum name) {
        this.name = name;
    }
}