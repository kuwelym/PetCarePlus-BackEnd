package petitus.petcareplus.model.profile;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import petitus.petcareplus.model.AbstractBaseEntity;

import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "service_provider_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProviderProfile extends AbstractBaseEntity{

    @OneToOne
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    @JsonBackReference
    private Profile profile;

    @ElementCollection
    @CollectionTable(name = "service_provider_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "skill")
    private Set<String> skills;

    @ElementCollection
    @CollectionTable(name = "service_provider_image_urls", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "image_url")
    private Set<String> imageUrls;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "available_time", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> availableTime;

    @Column(name = "rating", nullable = false)
    @Builder.Default
    private Double rating = 0.0;
}
