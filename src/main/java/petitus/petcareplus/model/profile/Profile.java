package petitus.petcareplus.model.profile;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import petitus.petcareplus.model.AbstractBaseEntity;
import petitus.petcareplus.model.User;

import java.time.LocalDate;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile extends AbstractBaseEntity {

    @OneToOne(mappedBy = "profile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ServiceProviderProfile serviceProviderProfile;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "is_service_provider")
    private boolean isServiceProvider;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "location")
    private String location;

    @Column(name = "about", columnDefinition = "TEXT")
    private String about;

    @Version
    private Long version;
}
